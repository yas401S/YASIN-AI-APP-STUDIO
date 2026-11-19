package com.yasin.aiappstudio

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class WebBuildRun(val id: Long, val status: String, val conclusion: String?, val htmlUrl: String)

/** Uploads a website project to an isolated branch and dispatches the repository web build workflow. */
object GitHubWebBuild {
    private const val API = "https://api.github.com"
    private const val OWNER = "yas401S"
    private const val REPO = "YASIN-AI-APP-STUDIO"
    private const val MAX_FILE_BYTES = 20L * 1024L * 1024L
    private const val MAX_FILES = 500
    private const val MAX_TOTAL_BYTES = 100L * 1024L * 1024L

    private fun request(token: String, method: String, path: String, body: JSONObject? = null): String {
        val connection = (URL(API + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20000
            readTimeout = 30000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "YASIN-AI-APP-STUDIO")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        return try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(text).optString("message") }.getOrDefault("")
                throw IllegalStateException("GitHub API ($code): " + message.ifBlank { "درخواست ناموفق بود." })
            }
            text
        } finally { connection.disconnect() }
    }

    fun uploadProjectAndDispatch(token: String, projectRoot: File, projectName: String): String {
        require(projectRoot.isDirectory) { "پوشه پروژه پیدا نشد." }
        val root = projectRoot.canonicalFile
        val files = root.walkTopDown().filter { file ->
            if (!file.isFile) return@filter false
            val relative = file.relativeTo(root).invariantSeparatorsPath
            val safe = runCatching { file.canonicalPath.startsWith(root.path + File.separator) }.getOrDefault(false)
            safe && !relative.split('/').any { it.startsWith(".") || it == "node_modules" || it == "dist" || it == "build" } &&
                file.length() <= MAX_FILE_BYTES
        }.toList()
        require(files.isNotEmpty()) { "پروژه فایل قابل ارسال ندارد." }
        require(files.size <= MAX_FILES) { "تعداد فایل‌ها بیش از حد مجاز است (حداکثر ۵۰۰ فایل)." }
        require(files.sumOf { it.length() } <= MAX_TOTAL_BYTES) { "حجم کل پروژه بیش از ۱۰۰ مگابایت است." }
        require(File(root, "index.html").isFile || File(root, "package.json").isFile) {
            "پروژه وب معتبر نیست؛ index.html یا package.json پیدا نشد."
        }
        val slug = projectName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(35).ifBlank { "web-app" }
        val branch = "studio-web-" + System.currentTimeMillis().toString()
        val ref = JSONObject(request(token, "GET", "/repos/$OWNER/$REPO/git/ref/heads/main"))
        val baseSha = ref.getJSONObject("object").getString("sha")
        request(token, "POST", "/repos/$OWNER/$REPO/git/refs",
            JSONObject().put("ref", "refs/heads/$branch").put("sha", baseSha))

        files.forEach { file ->
            val relative = file.relativeTo(root).invariantSeparatorsPath
            val path = "generated-projects/$slug/$relative"
            val encodedPath = path.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
            val content = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
            val payload = JSONObject()
                .put("message", "Upload $projectName web project file: $relative")
                .put("content", content)
                .put("branch", branch)
            request(token, "PUT", "/repos/$OWNER/$REPO/contents/$encodedPath", payload)
        }

        val dispatch = JSONObject()
            .put("ref", branch)
            .put("inputs", JSONObject()
                .put("project_path", "generated-projects/$slug")
                .put("project_name", slug))
        request(token, "POST", "/repos/$OWNER/$REPO/actions/workflows/project-web-build.yml/dispatches", dispatch)
        return branch
    }

    fun latestBuild(token: String, branch: String): WebBuildRun? {
        val encoded = URLEncoder.encode(branch, "UTF-8")
        val json = JSONObject(request(token, "GET",
            "/repos/$OWNER/$REPO/actions/runs?branch=$encoded&event=workflow_dispatch&per_page=10"))
        val runs = json.optJSONArray("workflow_runs") ?: JSONArray()
        if (runs.length() == 0) return null
        val run = runs.getJSONObject(0)
        return WebBuildRun(
            run.getLong("id"),
            run.optString("status"),
            run.optString("conclusion").takeIf { it != "null" && it.isNotBlank() },
            run.optString("html_url")
        )
    }
}
