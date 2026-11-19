package com.yasin.aiappstudio

import java.io.File

/**
 * Offline preview for static HTML projects. Local CSS and JavaScript are inlined;
 * network and file access are disabled in the WebView by the caller.
 */
object StaticPreviewRenderer {
    private const val MAX_HTML_BYTES = 512 * 1024
    private const val MAX_ASSET_BYTES = 256 * 1024

    fun render(workspace: File, projectName: String): String {
        require(projectName.isNotBlank()) { "ابتدا یک پروژه را انتخاب کنید." }
        val root = File(workspace, projectName).canonicalFile
        require(root.isDirectory && root.path.startsWith(workspace.canonicalPath + File.separator)) {
            "پوشه پروژه معتبر نیست."
        }
        val htmlFile = safeLocalFile(root, "index.html")
        require(htmlFile.isFile) { "فایل index.html در پروژه پیدا نشد. این پیش‌نمایش فعلاً برای قالب وب ایستا است." }
        require(htmlFile.length() <= MAX_HTML_BYTES) { "حجم index.html برای پیش‌نمایش بیش از حد مجاز است." }
        var html = htmlFile.readText()

        val cssPattern = Regex("""<link\b[^>]*href\s*=\s*["']([^"']+\.css)["'][^>]*>""", RegexOption.IGNORE_CASE)
        html = cssPattern.replace(html) { match ->
            val path = match.groupValues[1]
            val asset = safeLocalFile(root, path)
            require(asset.isFile && asset.length() <= MAX_ASSET_BYTES) { "فایل CSS محلی پیدا نشد یا بیش از حد بزرگ است: $path" }
            "<style>\n${asset.readText().replace("</style", "<\\/style", ignoreCase = true)}\n</style>"
        }

        val jsPattern = Regex("""<script\b([^>]*)src\s*=\s*["']([^"']+\.js)["']([^>]*)>\s*</script>""", RegexOption.IGNORE_CASE)
        html = jsPattern.replace(html) { match ->
            val path = match.groupValues[2]
            val asset = safeLocalFile(root, path)
            require(asset.isFile && asset.length() <= MAX_ASSET_BYTES) { "فایل JavaScript محلی پیدا نشد یا بیش از حد بزرگ است: $path" }
            "<script>\n${asset.readText().replace("</script", "<\\/script", ignoreCase = true)}\n</script>"
        }
        return html
    }

    private fun safeLocalFile(root: File, rawPath: String): File {
        require(rawPath.isNotBlank() && !rawPath.contains("://") && !rawPath.startsWith("//")) {
            "پیش‌نمایش فقط فایل‌های محلی پروژه را می‌پذیرد."
        }
        val normalized = rawPath.substringBefore('?').substringBefore('#').removePrefix("/")
        require(normalized.isNotBlank() && normalized.split('/').none { it == ".." || it == "." }) {
            "مسیر فایل دارایی معتبر نیست."
        }
        val file = File(root, normalized).canonicalFile
        require(file.path.startsWith(root.path + File.separator)) { "مسیر فایل از محدوده پروژه خارج شده است." }
        return file
    }
}
