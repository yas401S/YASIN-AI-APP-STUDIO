package com.yasin.aiappstudio

import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** Resolves only GET-able files contained in the selected project preview root. */
object PreviewAssetResolver {
    fun resolveFile(root: File, requestPath: String): File? = runCatching {
        val canonicalRoot = root.canonicalFile
        if (!canonicalRoot.isDirectory) return null
        val decoded = URLDecoder.decode(requestPath, StandardCharsets.UTF_8.name())
        if (decoded.isBlank() || decoded.contains(0.toChar())) return null
        val relative = decoded.removePrefix("/")
        if (relative.isBlank()) return null
        val target = File(canonicalRoot, relative).canonicalFile
        if (!target.path.startsWith(canonicalRoot.path + File.separator)) return null
        if (!target.isFile || !target.canRead()) return null
        target
    }.getOrNull()

    fun mimeType(file: File): String = when (file.extension.lowercase()) {
        "html", "htm" -> "text/html"
        "css" -> "text/css"
        "js", "mjs" -> "text/javascript"
        "json", "map" -> "application/json"
        "svg" -> "image/svg+xml"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "ico" -> "image/x-icon"
        "woff" -> "font/woff"
        "woff2" -> "font/woff2"
        "ttf" -> "font/ttf"
        "otf" -> "font/otf"
        "mp3" -> "audio/mpeg"
        "mp4" -> "video/mp4"
        "webm" -> "video/webm"
        "wasm" -> "application/wasm"
        "txt" -> "text/plain"
        else -> "application/octet-stream"
    }

    fun charsetFor(mimeType: String): String? =
        if (mimeType.startsWith("text/") || mimeType == "application/json" ||
            mimeType == "application/javascript") "UTF-8" else null
}
