const VISION_MODEL = "@cf/meta/llama-3.2-11b-vision-instruct";
const CODE_MODEL = "@cf/qwen/qwen2.5-coder-32b-instruct";
const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS, GET",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
  "Access-Control-Max-Age": "86400"
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      ...CORS,
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store"
    }
  });
}

async function hitLimit(env, ip) {
  const now = Date.now();
  const hour = Math.floor(now / 3600000);
  const day = new Date(now).toISOString().slice(0, 10);
  const hourKey = "h:" + ip + ":" + hour;
  const dayKey = "d:" + ip + ":" + day;
  const [hourCount, dayCount] = await Promise.all([
    env.RATE_LIMIT.get(hourKey),
    env.RATE_LIMIT.get(dayKey)
  ]);

  if (Number(hourCount || 0) >= 30 || Number(dayCount || 0) >= 150) {
    return false;
  }

  await Promise.all([
    env.RATE_LIMIT.put(hourKey, String(Number(hourCount || 0) + 1), { expirationTtl: 3700 }),
    env.RATE_LIMIT.put(dayKey, String(Number(dayCount || 0) + 1), { expirationTtl: 90000 })
  ]);
  return true;
}

function extractMessages(input) {
  if (!Array.isArray(input.messages) || input.messages.length < 1 || input.messages.length > 20) {
    throw new Error("messages must contain 1 to 20 entries");
  }

  let image = null;
  const messages = input.messages.map((message) => {
    const role = ["system", "user", "assistant"].includes(message?.role)
      ? message.role
      : "user";

    if (typeof message.content === "string") {
      return { role, content: message.content.slice(0, 50000) };
    }

    if (Array.isArray(message.content)) {
      const content = [];
      for (const part of message.content) {
        if (part?.type === "text" && typeof part.text === "string") {
          content.push({ type: "text", text: part.text.slice(0, 50000) });
        } else if (part?.type === "image_url") {
          const imageUrl = typeof part.image_url === "string"
            ? part.image_url
            : part.image_url?.url;
          if (typeof imageUrl === "string" && imageUrl.startsWith("data:image/")) {
            if (imageUrl.length > 7 * 1024 * 1024) {
              throw new Error("Image too large; maximum 5 MiB");
            }
            image ||= imageUrl;
            // Workers AI Llama Vision expects the image in the top-level image
            // input field, not as an OpenAI image_url content part.
          }
        }
      }
      return {
        role,
        content: content.length ? content : "Please inspect the attached image."
      };
    }

    return { role, content: "" };
  });

  return { messages, image };
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: CORS });
    }

    if (request.method === "GET" && url.pathname === "/health") {
      return json({ ok: true, service: "YASIN AI Studio", models: { vision: VISION_MODEL, textAndCode: CODE_MODEL } });
    }

    if (request.method !== "POST" || url.pathname !== "/v1/chat/completions") {
      return json({ error: { message: "Not found" } }, 404);
    }

    const contentLength = Number(request.headers.get("content-length") || 0);
    if (contentLength > 12 * 1024 * 1024) {
      return json({ error: { message: "Request too large; maximum 10 MiB" } }, 413);
    }

    const ip = request.headers.get("CF-Connecting-IP") || "unknown";

    try {
      if (!await hitLimit(env, ip)) {
        return json({ error: { message: "Rate limit reached. Please try again later." } }, 429);
      }

      const input = await request.json();
      const { messages, image } = extractMessages(input);

      if (image && image.length > 11 * 1024 * 1024) {
        return json({ error: { message: "Image too large" } }, 413);
      }

      const maxTokens = Math.max(128, Math.min(Number(input.max_tokens || 8192), 8192));
      // The 11B vision model handles image requests. The 70B text model supports
      // Cloudflare JSON Mode and is substantially more reliable for file generation.
      const model = image ? VISION_MODEL : CODE_MODEL;
      const aiInput = { messages, max_tokens: maxTokens, temperature: 0.15 };
      if (image) {
        // Cloudflare's documented Llama 3.2 Vision interface uses a top-level
        // image field containing a data URL or image URL.
        aiInput.image = image;
      } else if (["json_object", "json_schema"].includes(input.response_format?.type)) {
        aiInput.response_format = input.response_format;
      }

      let result;
      try {
        result = await env.AI.run(model, aiInput);
      } catch (firstError) {
        // Retry without constrained output if a schema edge case is rejected.
        // The Android client still validates JSON before proposing file writes.
        if (!image && aiInput.response_format) {
          const fallbackInput = { ...aiInput };
          delete fallbackInput.response_format;
          result = await env.AI.run(model, fallbackInput);
        } else {
          throw firstError;
        }
      }

      // A coding agent should review generated source before presenting it.
      // This second pass is limited to actual file-generation responses; normal
      // chat stays single-pass. If review fails, preserve the valid first draft.
      if (!image && aiInput.response_format && result?.response) {
        const firstDraft = typeof result.response === "string"
          ? result.response
          : JSON.stringify(result.response);
        let draftObject = null;
        try { draftObject = JSON.parse(firstDraft); } catch {}
        if (Array.isArray(draftObject?.files) && draftObject.files.length > 0) {
          try {
            const lastUserMessage = [...messages].reverse().find((item) => item.role === "user")?.content || "";
            const sourceContext = messages.find((item) => item.role === "system")?.content || "";
            const reviewMessages = [
              {
                role: "system",
                content: [
                  "You are an independent principal engineer performing a strict pre-delivery code review.",
                  "Review the proposed complete project files for compile/runtime errors, missing imports, unresolved symbols, incompatible APIs, incomplete interactions, unsafe behavior, broken paths, and contradictions between files.",
                  "Return the SAME strict JSON schema requested by the original task: {reply,summary,files}.",
                  "If the draft has defects, fix them in the complete file contents. Preserve correct work and existing architecture. Do not add TODOs, placeholders, fake data, fake success, or empty handlers.",
                  "Do not claim tests were run. Do not invent project dependencies or configuration. Keep every file path unchanged unless it is demonstrably wrong."
                ].join(" ")
              },
              {
                role: "user",
                content: "Original project context (may be abbreviated):\n" + sourceContext.slice(0, 14000) +
                  "\n\nUser request:\n" + lastUserMessage.slice(0, 12000) +
                  "\n\nReview and correct this generated JSON draft. Return complete corrected JSON only:\n" + firstDraft.slice(0, 42000)
              }
            ];
            const reviewInput = {
              messages: reviewMessages,
              max_tokens: maxTokens,
              temperature: 0.08
            };
            if (aiInput.response_format) reviewInput.response_format = aiInput.response_format;
            const reviewed = await env.AI.run(CODE_MODEL, reviewInput);
            const reviewedText = typeof reviewed?.response === "string"
              ? reviewed.response
              : JSON.stringify(reviewed?.response ?? reviewed);
            const reviewedObject = JSON.parse(reviewedText);
            if (Array.isArray(reviewedObject?.files) &&
                reviewedObject.files.length > 0 &&
                reviewedObject.files.every((file) =>
                  typeof file?.path === "string" &&
                  typeof file?.content === "string" &&
                  file.content.length > 0)) {
              result = {
                ...reviewed,
                usage: {
                  prompt_tokens: Number(result?.usage?.prompt_tokens || 0) + Number(reviewed?.usage?.prompt_tokens || 0),
                  completion_tokens: Number(result?.usage?.completion_tokens || 0) + Number(reviewed?.usage?.completion_tokens || 0),
                  total_tokens: Number(result?.usage?.total_tokens || 0) + Number(reviewed?.usage?.total_tokens || 0)
                }
              };
            }
          } catch (reviewError) {
            console.warn("AI code review pass failed; retaining first valid draft:", String(reviewError?.message || reviewError).slice(0, 180));
          }
        }
      }

      const content = typeof result?.response === "string"
        ? result.response
        : JSON.stringify(result?.response ?? result);
      const usage = result?.usage || {};

      return json({
        id: "yasin-ai-" + crypto.randomUUID(),
        object: "chat.completion",
        created: Math.floor(Date.now() / 1000),
        model,
        choices: [{
          index: 0,
          message: { role: "assistant", content },
          finish_reason: "stop"
        }],
        usage: {
          prompt_tokens: Number(usage.prompt_tokens || 0),
          completion_tokens: Number(usage.completion_tokens || 0),
          total_tokens: Number(usage.total_tokens || 0)
        }
      });
    } catch (error) {
      return json({
        error: { message: String(error?.message || "AI request failed").slice(0, 500) }
      }, 502);
    }
  }
};
