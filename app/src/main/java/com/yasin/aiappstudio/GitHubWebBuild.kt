package com.yasin.aiappstudio

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.zip.ZipInputStream

data class WebBuildSubmission(val branch: String, val projectSlug: String)
data class WebBuildRun(
    val id: Long,
    val status: String,
    val conclusion: String?,
    val htmlUrl: String,
    val buildSucceeded: Boolean,
    val artifactId: Long?,
    val previewUrl: String?,
    val deploymentConclusion: String?
)

/** Uploads web sources to an isolated branch, dispatches CI, and safely downloads its build artifact. */
object GitHubWebBuild {
    private const val API = "https://api.github.com"
    private const val OWNER = "yas401S"
    private const val REPO = "YASIN-AI-APP-STUDIO"
    private const val MAX_FILE_BYTES = 20L * 1024L * 1024L
    private const val MAX_FILES = 500
    private const val MAX_TOTAL_BYTES = 100L * 1024L * 1024L
    private const val MAX_ARTIFACT_FILES = 20000
    private const val MAX_ARTIFACT_BYTES = 200L * 1024L * 1024L

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

    fun uploadProjectAndDispatch(token: String, projectRoot: File, projectName: String): WebBuildSubmission {
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
        val readableSlug = projectName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(26).ifBlank { "web-app" }
        val slug = "$readableSlug-${Integer.toHexString(projectName.hashCode()).takeLast(8)}"
        val branch = "studio-web-" + System.currentTimeMillis().toString()
        val ref = JSONObject(request(token, "GET", "/repos/$OWNER/$REPO/git/ref/heads/main"))
        val baseSha = ref.getJSONObject("object").getString("sha")
        request(token, "POST", "/repos/$OWNER/$REPO/git/refs",
            JSONObject().put("ref", "refs/heads/$branch").put("sha", baseSha))

        files.forEach { file ->
            val relative = file.relativeTo(root).invariantSeparatorsPath
            val path = "generated-projects/$slug/$relative"
            val encodedPath = path.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
            val encodedContent = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
            val payload = JSONObject()
                .put("message", "Upload $projectName web project file: $relative")
                .put("content", encodedContent)
                .put("branch", branch)
            request(token, "PUT", "/repos/$OWNER/$REPO/contents/$encodedPath", payload)
        }

        val dispatch = JSONObject()
            .put("ref", branch)
            .put("inputs", JSONObject()
                .put("project_path", "generated-projects/$slug")
                .put("project_name", slug))
        request(token, "POST", "/repos/$OWNER/$REPO/actions/workflows/project-web-build.yml/dispatches", dispatch)
        return WebBuildSubmission(branch, slug)
    }

    fun latestBuild(token: String, branch: String, projectSlug: String): WebBuildRun? {
        val encoded = URLEncoder.encode(branch, "UTF-8")
        val json = JSONObject(request(token, "GET",
            "/repos/$OWNER/$REPO/actions/runs?branch=$encoded&event=workflow_dispatch&per_page=10"))
        val runs = json.optJSONArray("workflow_runs") ?: JSONArray()
        if (runs.length() == 0) return null
        val run = runs.getJSONObject(0)
        val runId = run.getLong("id")
        val jobsJson = JSONObject(request(token, "GET", "/repos/$OWNER/$REPO/actions/runs/$runId/jobs?per_page=100"))
        val jobs = jobsJson.optJSONArray("jobs") ?: JSONArray()
        var buildSucceeded = false
        var deployConclusion: String? = null
        var previewUrl: String? = null
        for (i in 0 until jobs.length()) {
            val job = jobs.getJSONObject(i)
            when (job.optString("name")) {
                "build" -> buildSucceeded = job.optString("conclusion") == "success"
                "deploy-preview" -> {
                    deployConclusion = job.optString("conclusion").takeIf { it.isNotBlank() && it != "null" }
                    if (deployConclusion == "success") {
                        val safeBranch = branch.lowercase().replace(Regex("[^a-z0-9-]"), "-")
                        previewUrl = "https://$safeBranch.$projectSlug.pages.dev"
                    }
                }
            }
        }
        val artifactsJson = JSONObject(request(token, "GET", "/repos/$OWNER/$REPO/actions/runs/$runId/artifacts"))
        val artifacts = artifactsJson.optJSONArray("artifacts") ?: JSONArray()
        var artifactId: Long? = null
        for (i in 0 until artifacts.length()) {
            val artifact = artifacts.getJSONObject(i)
            if (artifact.optString("name") == "web-dist-$projectSlug" && !artifact.optBoolean("expired")) {
                artifactId = artifact.optLong("id").takeIf { it > 0 }
                break
            }
        }
        return WebBuildRun(
            runId,
            run.optString("status"),
            run.optString("conclusion").takeIf { it != "null" && it.isNotBlank() },
            run.optString("html_url"),
            buildSucceeded,
            artifactId,
            previewUrl,
            deployConclusion
        )
    }

    /** Downloads a GitHub Actions ZIP through its short-lived redirect and extracts only safe files. */
    fun downloadArtifact(token: String, artifactId: Long, projectRoot: File) {
        require(artifactId > 0) { "شناسه خروجی معتبر نیست." }
        require(projectRoot.isDirectory) { "پوشه پروژه پیدا نشد." }
        val apiConnection = (URL("$API/repos/$OWNER/$REPO/actions/artifacts/$artifactId/zip").openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 20000
            readTimeout = 30000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "YASIN-AI-APP-STUDIO")
        }
        val downloadUrl = try {
            val code = apiConnection.responseCode
            require(code in 300..399) { "GitHub فایل خروجی را برای دانلود آماده نکرد (HTTP $code)." }
            apiConnection.getHeaderField("Location") ?: error("نشانی دانلود خروجی دریافت نشد.")
        } finally { apiConnection.disconnect() }

        val root = projectRoot.canonicalFile
        val staging = File(root, ".web-preview-${System.currentTimeMillis()}")
        require(staging.mkdirs()) { "ساخت پوشه موقت پیش‌نمایش ناموفق بود." }
        try {
            val connection = URL(downloadUrl).openConnection().apply {
                connectTimeout = 20000
                readTimeout = 60000
            }
            var totalBytes = 0L
            var fileCount = 0
            ZipInputStream(BufferedInputStream(connection.getInputStream())).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    require(name.isNotBlank() && !name.startsWith("/") && !name.contains('\u0000')) {
                        "مسیر نامعتبر در فایل خروجی پیدا شد."
                    }
                    val target = File(staging, name).canonicalFile
                    require(target.path.startsWith(staging.canonicalPath + File.separator)) {
                        "مسیر ناامن داخل فایل خروجی رد شد."
                    }
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        fileCount++
                        require(fileCount <= MAX_ARTIFACT_FILES) { "تعداد فایل‌های خروجی بیش از حد مجاز است." }
                        target.parentFile?.mkdirs()
                        target.outputStream().buffered().use { output ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                totalBytes += count
                                require(totalBytes <= MAX_ARTIFACT_BYTES) { "حجم فایل خروجی بیش از ۲۰۰ مگابایت است." }
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
            require(File(staging, "index.html").isFile) { "خروجی دانلودشده index.html ندارد." }
            val dist = File(root, "dist")
            if (dist.exists()) require(dist.deleteRecursively()) { "پاک‌کردن خروجی قبلی ممکن نشد." }
            require(staging.renameTo(dist)) { "انتقال خروجی به پوشه dist ناموفق بود." }
        } catch (error: Exception) {
            staging.deleteRecursively()
            throw error
        }
    }
}
