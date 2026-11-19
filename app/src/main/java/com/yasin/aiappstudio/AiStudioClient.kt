package com.yasin.aiappstudio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayOutputStream

internal data class StudioChatMessage(val role: String, val content: String)
internal data class StudioFilePatch(val path: String, val content: String)
internal data class StudioAiResult(val reply: String, val summary: String = "", val patches: List<StudioFilePatch> = emptyList(), val usage: AiUsage? = null)

private fun prepareStudioImage(context: Context, uri: Uri): String {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: error("خواندن تصویر ناموفق بود.")
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "فایل تصویر معتبر نیست." }

    var sample = 1
    while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > 1600) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.RGB_565 }
    val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: error("رمزگشایی تصویر ناموفق بود.")
    val output = java.io.ByteArrayOutputStream()
    try {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, output)
        val bytes = output.toByteArray()
        require(bytes.isNotEmpty() && bytes.size <= 5 * 1024 * 1024) {
            "آماده‌سازی تصویر ناموفق بود؛ لطفاً تصویر دیگری انتخاب کن."
        }
        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    } finally {
        bitmap.recycle()
        output.close()
    }
}

private suspend fun analyzeStudioImage(
    endpoint: String, model: String, key: String, imageDataUrl: String
): String = withContext(Dispatchers.IO) {
    require(endpoint.isNotBlank() && model.isNotBlank()) { "نشانی اتصال مدل بینایی تنظیم نشده است." }
    val safeEndpoint = AiEndpointPolicy.validate(endpoint)
    var conn: HttpURLConnection? = null
    try {
        conn = (URL(safeEndpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 120000
            doOutput = true
            instanceFollowRedirects = false
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (key.isNotBlank()) setRequestProperty("Authorization", "Bearer $key")
        }
        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content",
            "You are a senior computer-vision and UI reverse-engineering specialist. Inspect the supplied image carefully. Return STRICT JSON only: {\"analysis\":\"...\"}. Describe visible screen structure, hierarchy, components, spacing, alignment, colors with approximate hex values, typography, icons, controls, navigation, states, interactions inferable from the image, and responsive behavior. Separate observed facts from uncertain assumptions. Be detailed enough that a coding agent can recreate the interface. Do not return source code."))
        val content = JSONArray()
            .put(JSONObject().put("type", "text").put("text", "تحلیل بینایی دقیق این تصویر را انجام بده. تمام اجزای قابل مشاهده و ساختار صفحه را برای بازسازی دقیق استخراج کن."))
            .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", imageDataUrl)))
        messages.put(JSONObject().put("role", "user").put("content", content))
        val payload = JSONObject().put("model", model).put("messages", messages)
            .put("response_format", JSONObject().put("type", "json_object"))
        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val status = conn.responseCode
        val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        val raw = stream.bufferedReader().use { it.readText().take(2 * 1024 * 1024) }
        if (status !in 200..299) error("خطای سرویس بینایی ($status): " + raw.take(700))
        val response = JSONObject(raw)
        val message = response.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content")
        val cleaned = message.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val structuredAnalysis = runCatching { JSONObject(cleaned).optString("analysis") }.getOrNull()
        structuredAnalysis?.takeIf { it.isNotBlank() }
            ?: cleaned.takeIf { it.isNotBlank() }
            ?: error("مدل بینایی تحلیل قابل استفاده‌ای برنگرداند.")
    } finally {
        conn?.disconnect()
    }
}

private suspend fun askStudioModel(
    endpoint: String, model: String, key: String, prompt: String,
    history: List<StudioChatMessage>, projectContext: String, imageDataUrl: String?
): StudioAiResult = withContext(Dispatchers.IO) {
    if (endpoint.isBlank() || model.isBlank()) return@withContext StudioAiResult("نشانی اتصال هوش مصنوعی تنظیم نشده است.")
    val safeEndpoint = runCatching { AiEndpointPolicy.validate(endpoint) }.getOrElse {
        return@withContext StudioAiResult("آدرس API نامعتبر است: " + (it.message ?: "فقط HTTPS مجاز است."))
    }
    var conn: HttpURLConnection? = null
    try {
        conn = (URL(safeEndpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 20000; readTimeout = 180000; doOutput = true
            instanceFollowRedirects = false
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (key.isNotBlank()) setRequestProperty("Authorization", "Bearer $key")
        }
        val messages = JSONArray()
        val systemPrompt = """You are YASIN AI STUDIO, a principal software engineer, solution architect, senior product designer, QA engineer and autonomous coding agent. Your user speaks Persian; all user-facing explanations must be clear, natural Persian.
ENGINEERING WORKFLOW:
1. Treat this screen as an implementation-first coding studio. When the user asks to create, build, add, change, fix, design, or turn an idea/image into software, produce actual complete project files in the selected project's existing architecture. Infer the target platform and framework from the request; when unspecified, default to a native Android Kotlin + Jetpack Compose app. Make sensible product decisions and begin immediately. Ask only when a missing fact would materially change the architecture or make implementation impossible. Never answer a clear build request with advice alone or an empty files array.
2. Inspect every relevant existing project file supplied in context before changing anything. Identify language, framework, package names, dependencies, entry points and existing conventions. Extend the current project; do not replace it with a toy template.
3. Plan a coherent implementation internally. Build complete connected features, not isolated mock screens. Every visible button, menu, form, navigation item and interaction must perform its stated function.
4. Implement production-minded code: correct architecture, reusable components, state management, validation, loading/empty/error states, accessibility, responsive layouts, secure data handling, and graceful failure. Use real APIs only when credentials/configuration are available; never invent successful network data.
5. Add or update tests for core logic and important user flows when the project supports tests. Include setup/configuration/documentation only when required.
6. Review your own output before returning it: check imports, symbols, dependency versions, package paths, nullability, async/threading, lifecycle, navigation, permissions, API contracts, edge cases and consistency between files. Fix issues you detect. Never knowingly return incomplete or non-compiling code.
7. Prefer a small set of complete, integrated files over many disconnected files. Do not omit required assets/configuration. If the output budget prevents completion, state exactly what remains instead of pretending the app is complete.
OUTPUT CONTRACT:
- When creating or modifying files, return STRICT JSON only:
{"reply":"Persian user-facing response","summary":"short implementation summary","files":[{"path":"relative/path","content":"complete file content"}]}
- For ordinary questions, return {"reply":"...","summary":"","files":[]}.
- Every returned file must contain its complete final content, never a diff, ellipsis, TODO placeholder, fake implementation or pseudocode.
- Use only safe paths relative to the selected project root. Never emit absolute paths, .. segments, hidden paths, secrets, or executable shell scripts.
- Never overwrite build outputs, .git, .history, Gradle caches, local.properties, keystores, signing configs or credential files.
- Preserve existing build configuration unless a dependency/configuration change is necessary; explain required additions in the Persian reply.
QUALITY BAR:
- Do not use placeholder text, fake buttons, hard-coded demo records, simulated success, or empty event handlers.
- Do not claim a feature is complete if only its UI exists. Connect UI to working logic and real persistence/API behavior.
- For every explicit build/create/implement request, return all required source, resource, configuration, and documentation files needed for a coherent runnable project. Include tests for critical logic when the framework supports them. Do not return an empty files array for a clear implementation request; if the output budget or missing external tool prevents completion, return the best complete vertical slice and state the exact remaining work.
- Prefer a complete, usable first version with real flows over a broad but superficial collection of screens. Implement the core user journey end-to-end before optional features.
- For APIs, implement loading, pagination where appropriate, timeout, retry policy, validation and readable errors. Never expose secrets in client code.
- For data-changing actions, validate input and confirm destructive actions.
- For web apps, build a polished responsive interface for mobile and desktop with semantic HTML, keyboard support and accessible contrast.
- For Android, respect lifecycle, permissions, background threading, Material 3 conventions and the existing Gradle/dependency versions.
- For games, implement an actual playable loop, input, state, collision/rules and restart flow; do not deliver a static mockup.
- For image-to-app tasks, separate observed facts from assumptions, reproduce layout hierarchy, spacing, typography, colors, icons and responsive behavior, and implement the interactions implied by the design. Never invent unseen screens as if they were shown.
For image requests, inspect the visual specification and then create real source files consistent with the existing project's language and architecture.
For the supplied Android Kotlin + Jetpack Compose template, preserve the exact package declaration and edit the existing app/src/main/java/.../MainActivity.kt path shown in project context. Use androidx.compose.material3 APIs, match actual dependencies in app/build.gradle.kts, and return complete compilable Kotlin files. Do not invent XML layouts for a Compose project.
Current selected project files:
$projectContext"""
        messages.put(JSONObject().put("role", "system").put("content", systemPrompt))
        history.takeLast(16).forEach { item ->
            if (item.role == "user" || item.role == "assistant") messages.put(JSONObject().put("role", item.role).put("content", item.content.take(10000)))
        }
        val userContent: Any = if (imageDataUrl.isNullOrBlank()) prompt else JSONArray()
            .put(JSONObject().put("type", "text").put("text", prompt.ifBlank { "این تصویر را به رابط کاربری واقعی در پروژه تبدیل کن." }))
            .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", imageDataUrl)))
        messages.put(JSONObject().put("role", "user").put("content", userContent))
        val fileSchema = JSONObject()
            .put("type", "object")
            .put("properties", JSONObject()
                .put("path", JSONObject().put("type", "string"))
                .put("content", JSONObject().put("type", "string")))
            .put("required", JSONArray().put("path").put("content"))
            .put("additionalProperties", false)
        val outputSchema = JSONObject()
            .put("type", "object")
            .put("properties", JSONObject()
                .put("reply", JSONObject().put("type", "string"))
                .put("summary", JSONObject().put("type", "string"))
                .put("files", JSONObject().put("type", "array").put("items", fileSchema)))
            .put("required", JSONArray().put("reply").put("summary").put("files"))
            .put("additionalProperties", false)
        val responseFormat = JSONObject()
            .put("type", "json_schema")
            .put("json_schema", outputSchema)
        val payload = JSONObject().put("model", model).put("messages", messages)
            .put("max_tokens", 8192)
            .put("response_format", responseFormat)
        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val status = conn.responseCode
        val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        val raw = stream.bufferedReader().use { it.readText().take(2 * 1024 * 1024) }
        if (status !in 200..299) return@withContext StudioAiResult("خطای سرویس ($status): " + raw.take(1000))
        val response = JSONObject(raw)
        val content = response.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content")
        val jsonText = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val parsed = runCatching { JSONObject(jsonText) }.getOrNull()
            ?: return@withContext StudioAiResult(jsonText.take(12000), usage = AiUsageParser.parse(raw))
        val patches = mutableListOf<StudioFilePatch>()
        val files = parsed.optJSONArray("files") ?: JSONArray()
        require(files.length() <= 40) { "تعداد فایل‌های پیشنهادی بیش از حد مجاز است." }
        for (i in 0 until files.length()) {
            val item = files.getJSONObject(i)
            val path = item.optString("path")
            val body = item.optString("content")
            val parts = path.split('/')
            require(path.isNotBlank() && !path.startsWith('/') && !path.contains('\\') &&
                parts.none { it.isBlank() || it == "." || it == ".." || it.startsWith(".") }) { "مسیر فایل ناامن است: $path" }
            require(!path.startsWith("build/") && !path.startsWith(".git/") &&
                path !in listOf("local.properties", "gradle.properties", "settings.gradle", "settings.gradle.kts") &&
                !path.contains("keystore", true) && !path.contains("secret", true) && !path.contains("credential", true)) {
                "تغییر فایل حساس مسدود شد: $path"
            }
            require(body.toByteArray(Charsets.UTF_8).size <= 1024 * 1024) { "حجم فایل پیشنهادی بیش از حد مجاز است." }
            patches += StudioFilePatch(path, body)
        }
        StudioAiResult(parsed.optString("reply", "پاسخ آماده شد."), parsed.optString("summary"), patches, AiUsageParser.parse(raw))
    } catch (e: Exception) {
        StudioAiResult("نتوانستم پاسخ ساختاریافته را پردازش کنم: " + (e.message ?: "خطای نامشخص"))
    } finally { conn?.disconnect() }
}
