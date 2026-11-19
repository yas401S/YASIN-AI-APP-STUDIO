package com.yasin.aiappstudio

import java.io.File
import java.util.Base64

/**
 * Offline preview for static web projects. Local CSS, JavaScript and common
 * media assets are embedded so the WebView never needs filesystem access.
 */
object StaticPreviewRenderer {
    private const val MAX_HTML_BYTES = 512 * 1024
    private const val MAX_ASSET_BYTES = 2 * 1024 * 1024
    private const val MAX_TOTAL_ASSET_BYTES = 8 * 1024 * 1024

    fun render(workspace: File, projectName: String): String {
        require(projectName.isNotBlank()) { "ابتدا یک پروژه را انتخاب کنید." }
        val base = workspace.canonicalFile
        val root = File(base, projectName).canonicalFile
        require(root.isDirectory && root.path.startsWith(base.path + File.separator)) {
            "پوشه پروژه معتبر نیست."
        }
        val htmlFile = safeLocalFile(root, "index.html")
        require(htmlFile.isFile) { "فایل index.html در پروژه پیدا نشد." }
        require(htmlFile.length() <= MAX_HTML_BYTES) { "حجم index.html برای پیش‌نمایش بیش از حد مجاز است." }

        var totalAssets = 0L
        fun account(file: File) {
            totalAssets += file.length()
            require(file.length() <= MAX_ASSET_BYTES && totalAssets <= MAX_TOTAL_ASSET_BYTES) {
                "حجم دارایی‌های پروژه برای پیش‌نمایش بیش از حد مجاز است."
            }
        }

        var html = htmlFile.readText()
        val cssPattern = Regex("""<link\b([^>]*?)href\s*=\s*["']([^"']+\.css(?:\?[^"']*)?)["']([^>]*)>""", RegexOption.IGNORE_CASE)
        html = cssPattern.replace(html) { match ->
            val path = match.groupValues[2]
            val asset = safeLocalFile(root, path)
            require(asset.isFile) { "فایل CSS محلی پیدا نشد: $path" }
            account(asset)
            "<style>\n${embedCss(asset.readText(), root, asset.parentFile)}\n</style>"
        }

        val jsPattern = Regex("""<script\b([^>]*?)src\s*=\s*["']([^"']+\.js(?:\?[^"']*)?)["']([^>]*)>\s*</script>""", RegexOption.IGNORE_CASE)
        html = jsPattern.replace(html) { match ->
            val path = match.groupValues[2]
            val asset = safeLocalFile(root, path)
            require(asset.isFile) { "فایل JavaScript محلی پیدا نشد: $path" }
            account(asset)
            "<script>\n${asset.readText().replace("</script", "<\\/script", ignoreCase = true)}\n</script>"
        }

        val mediaPattern = Regex("""(\b(?:src|poster)\s*=\s*["'])([^"']+)(["'])""", RegexOption.IGNORE_CASE)
        html = mediaPattern.replace(html) { match ->
            val raw = match.groupValues[2]
            if (raw.startsWith("data:", true) || raw.startsWith("#")) return@replace match.value
            val asset = safeLocalFile(root, raw)
            require(asset.isFile) { "فایل رسانه محلی پیدا نشد: $raw" }
            account(asset)
            match.groupValues[1] + dataUri(asset) + match.groupValues[3]
        }
        return html
    }

    private fun embedCss(css: String, root: File, cssDirectory: File): String {
        val pattern = Regex("""url\(\s*(['"]?)(.*?)\1\s*\)""", RegexOption.IGNORE_CASE)
        return pattern.replace(css) { match ->
            val raw = match.groupValues[2].trim()
            if (raw.isBlank() || raw.startsWith("data:", true) || raw.startsWith("#")) return@replace match.value
            require(!raw.contains("://") && !raw.startsWith("//")) {
                "پیش‌نمایش آفلاین فقط دارایی‌های محلی را می‌پذیرد: $raw"
            }
            val asset = safeLocalFile(root, raw, cssDirectory)
            require(asset.isFile) { "دارایی CSS پیدا نشد: $raw" }
            "url('${dataUri(asset)}')"
        }
    }

    private fun dataUri(file: File): String {
        val mime = when (file.extension.lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "ico" -> "image/x-icon"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "ttf" -> "font/ttf"
            "mp3" -> "audio/mpeg"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            else -> "application/octet-stream"
        }
        return "data:$mime;base64," + Base64.getEncoder().encodeToString(file.readBytes())
    }

    private fun safeLocalFile(root: File, rawPath: String, relativeTo: File = root): File {
        require(rawPath.isNotBlank() && !rawPath.contains("://") && !rawPath.startsWith("//")) {
            "پیش‌نمایش فقط فایل‌های محلی پروژه را می‌پذیرد."
        }
        val path = rawPath.substringBefore('?').substringBefore('#')
        require(path.isNotBlank())
        val target = File(if (path.startsWith("/")) root else relativeTo, path.removePrefix("/")).canonicalFile
        require(target.path.startsWith(root.path + File.separator)) {
            "مسیر فایل از محدوده پروژه خارج شده است."
        }
        return target
    }
}
