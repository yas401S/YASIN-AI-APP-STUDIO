package com.yasin.aiappstudio

import java.io.File

/**
 * Validates the entry point for a local web preview.
 * Subresources are served separately by PreviewAssetResolver through WebViewClient,
 * preserving ES modules, Vite chunks, CSS URLs and source maps.
 */
object StaticPreviewRenderer {
    private const val MAX_HTML_BYTES = 1024 * 1024

    fun previewRoot(workspace: File, projectName: String): File {
        require(projectName.isNotBlank()) { "ابتدا یک پروژه را انتخاب کنید." }
        val base = workspace.canonicalFile
        val root = File(base, projectName).canonicalFile
        require(root.isDirectory && root.path.startsWith(base.path + File.separator)) {
            "پوشه پروژه معتبر نیست."
        }
        val distRoot = File(root, "dist").canonicalFile
        return if (distRoot.isDirectory && File(distRoot, "index.html").isFile) distRoot else root
    }

    fun render(workspace: File, projectName: String): String {
        val root = previewRoot(workspace, projectName)
        val htmlFile = File(root, "index.html").canonicalFile
        require(htmlFile.path.startsWith(root.path + File.separator) && htmlFile.isFile) {
            "فایل index.html یا خروجی dist/index.html در پروژه پیدا نشد."
        }
        require(htmlFile.length() <= MAX_HTML_BYTES) {
            "حجم index.html برای پیش‌نمایش بیش از حد مجاز است."
        }
        return htmlFile.readText()
    }
}
