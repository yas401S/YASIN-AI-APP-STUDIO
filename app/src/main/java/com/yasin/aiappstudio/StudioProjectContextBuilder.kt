package com.yasin.aiappstudio

import java.io.File

/**
 * Builds a bounded, deterministic context snapshot for the coding agent.
 * Keeps project discovery and file I/O out of the Compose UI layer.
 */
object StudioProjectContextBuilder {
    private val supportedExtensions = setOf(
        "kt", "java", "xml", "gradle", "kts", "properties", "toml", "json",
        "yaml", "yml", "js", "jsx", "ts", "tsx", "html", "css", "scss",
        "md", "txt", "py", "swift", "dart", "go", "rs", "c", "cpp", "h", "hpp"
    )
    private val excludedDirectories = setOf("build", "node_modules", ".gradle", "dist")

    fun build(
        projectRoot: File,
        maxFiles: Int = 60,
        maxCharacters: Int = 48_000,
        maxFileCharacters: Int = 24_000
    ): String {
        if (!projectRoot.isDirectory || maxFiles <= 0 || maxCharacters <= 0 || maxFileCharacters <= 0) return ""

        val files = projectRoot.walkTopDown()
            .filter { file ->
                file.isFile &&
                    file.extension.lowercase() in supportedExtensions &&
                    file.relativeTo(projectRoot).path.split(File.separator).none { part ->
                        part.startsWith(".") || part in excludedDirectories
                    }
            }
            .sortedWith(compareBy<File>({ priority(projectRoot, it) }, {
                it.relativeTo(projectRoot).invariantSeparatorsPath.length
            }))
            .take(maxFiles)
            .toList()

        val context = StringBuilder()
        for (file in files) {
            if (context.length >= maxCharacters) break
            val relative = file.relativeTo(projectRoot).invariantSeparatorsPath
            val content = runCatching {
                if (file.length() > maxFileCharacters) "[فایل بزرگ است؛ محتوای آن ارسال نشد]"
                else file.readText(Charsets.UTF_8).take(maxFileCharacters)
            }.getOrElse { "[خواندن فایل ناموفق بود]" }
            val block = "FILE: $relative\n$content\n\n"
            val remaining = maxCharacters - context.length
            context.append(block.take(remaining))
        }
        return context.toString()
    }

    private fun priority(root: File, file: File): Int {
        val path = file.relativeTo(root).invariantSeparatorsPath.lowercase()
        return when {
            path == "settings.gradle" || path == "settings.gradle.kts" -> 0
            path == "build.gradle" || path == "build.gradle.kts" -> 1
            path.endsWith("/build.gradle") || path.endsWith("/build.gradle.kts") -> 2
            path.endsWith("gradle/libs.versions.toml") -> 2
            path.endsWith("androidmanifest.xml") -> 3
            path.endsWith("mainactivity.kt") || path.endsWith("mainactivity.java") -> 4
            path.contains("/src/main/") -> 5
            path.contains("/src/test/") || path.contains("/src/androidtest/") -> 6
            path == "package.json" || path == "vite.config.ts" || path == "vite.config.js" -> 2
            path == "readme.md" -> 7
            else -> 8
        }
    }
}
