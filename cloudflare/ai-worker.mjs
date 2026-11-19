const MODEL = "@cf/meta/llama-3.2-11b-vision-instruct";
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
      const textParts = [];
      for (const part of message.content) {
        if (part?.type === "text" && typeof part.text === "string") {
          textParts.push(part.text);
        }
        if (part?.type === "image_url") {
          const imageUrl = typeof part.image_url === "string"
            ? part.image_url
            : part.image_url?.url;
          if (typeof imageUrl === "string" && imageUrl.startsWith("data:image/")) {
            image ||= imageUrl;
          }
        }
      }
      return {
        role,
        content: textParts.join("\n").slice(0, 50000) || "Please inspect the attached image."
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
      return json({ ok: true, service: "YASIN AI Studio", model: MODEL });
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

      const maxTokens = Math.max(64, Math.min(Number(input.max_tokens || 4096), 4096));
      const aiInput = { messages, max_tokens: maxTokens, temperature: 0.2 };
      if (image) aiInput.image = image;

      const result = await env.AI.run(MODEL, aiInput);
      const content = typeof result?.response === "string"
        ? result.response
        : JSON.stringify(result?.response ?? result);
      const usage = result?.usage || {};

      return json({
        id: "yasin-ai-" + crypto.randomUUID(),
        object: "chat.completion",
        created: Math.floor(Date.now() / 1000),
        model: MODEL,
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
