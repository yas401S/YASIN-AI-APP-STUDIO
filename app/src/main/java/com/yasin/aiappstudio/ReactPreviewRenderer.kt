package com.yasin.aiappstudio

import java.io.File

/** Restricted preview for the generated single-file React + JSX starter. */
object ReactPreviewRenderer {
    private const val MAX_SOURCE_BYTES = 512 * 1024
    private const val MAX_STYLE_BYTES = 256 * 1024

    const val REACT_URL = "https://cdn.jsdelivr.net/npm/react@18.3.1/umd/react.production.min.js"
    const val REACT_DOM_URL = "https://cdn.jsdelivr.net/npm/react-dom@18.3.1/umd/react-dom.production.min.js"
    const val BABEL_URL = "https://cdn.jsdelivr.net/npm/@babel/standalone@7.26.10/babel.min.js"

    fun isReactProject(workspace: File, projectName: String): Boolean = runCatching {
        val root = projectRoot(workspace, projectName)
        safeLocalFile(root, "src/App.jsx").isFile && safeLocalFile(root, "package.json").isFile
    }.getOrDefault(false)

    fun isAllowedAsset(url: String): Boolean =
        url == REACT_URL || url == REACT_DOM_URL || url == BABEL_URL

    fun render(workspace: File, projectName: String): String {
        val root = projectRoot(workspace, projectName)
        val packageFile = safeLocalFile(root, "package.json")
        val appFile = safeLocalFile(root, "src/App.jsx")
        require(packageFile.isFile && appFile.isFile) {
            "پروژه React معتبر نیست؛ package.json و src/App.jsx باید وجود داشته باشند."
        }
        require(packageFile.length() <= 64 * 1024) { "package.json بیش از حد بزرگ است." }
        val packageJson = packageFile.readText()
        require(Regex("\"react\"\\s*:\\s*\"18.3.1\"").containsMatchIn(packageJson) &&
            Regex("\"react-dom\"\\s*:\\s*\"18.3.1\"").containsMatchIn(packageJson)) {
            "پیش‌نمایش فعلاً با نسخه React 18.3.1 قالب استودیو سازگار است."
        }
        require(appFile.length() <= MAX_SOURCE_BYTES) { "فایل src/App.jsx بیش از حد بزرگ است." }

        var source = appFile.readText()
        val importPattern = Regex("""(?m)^\s*import\s+[^;\r\n]+;?\s*$""")
        source = importPattern.replace(source) { match ->
            require(Regex("""\bfrom\s+['"]react['"]""").containsMatchIn(match.value)) {
                "پیش‌نمایش داخل استودیو فعلاً فقط import از react را پشتیبانی می‌کند؛ فایل‌های چندماژوله را با Vite اجرا کنید."
            }
            ""
        }
        require(!Regex("""(?m)^\s*import\s+""").containsMatchIn(source)) {
            "برای پیش‌نمایش داخل استودیو، import فایل‌های دیگر پشتیبانی نمی‌شود."
        }
        source = source.replace(Regex("""(?m)^\s*export\s+default\s+"""), "")
        require(Regex("""\b(?:function\s+App|const\s+App\s*=|class\s+App\b)""").containsMatchIn(source)) {
            "در src/App.jsx باید کامپوننتی با نام App تعریف شده باشد."
        }

        val styleFile = safeLocalFile(root, "src/style.css")
        val css = if (styleFile.isFile) {
            require(styleFile.length() <= MAX_STYLE_BYTES) { "فایل CSS بیش از حد بزرگ است." }
            styleFile.readText().replace("</style", "<\\/style", ignoreCase = true)
        } else ""
        val title = root.name.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        return """<!doctype html>
<html lang="fa" dir="rtl">
<head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net; style-src 'unsafe-inline'; img-src data: blob:; font-src data:; connect-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'; frame-src 'none'">
<title>$title · React Preview</title>
<style>$css</style>
</head>
<body>
<div id="root"></div><pre id="preview-error" role="alert" style="display:none;white-space:pre-wrap;color:#b91c1c;padding:16px"></pre>
<script src="$REACT_URL"></script>
<script src="$REACT_DOM_URL"></script>
<script src="$BABEL_URL"></script>
<script>
(function () {
  const source = __SOURCE_LITERAL__;
  try {
    const compiled = Babel.transform(source, { presets: ['react'] }).code;
    const createComponent = new Function('React', 'ReactDOM',
      'const { useState, useEffect, useMemo, useCallback, useRef, useReducer, useContext } = React;\n' +
      compiled + '\n; return App;');
    const App = createComponent(React, ReactDOM);
    ReactDOM.createRoot(document.getElementById('root')).render(React.createElement(App));
  } catch (error) {
    const box = document.getElementById('preview-error');
    box.style.display = 'block';
    box.textContent = 'خطای اجرای React: ' + (error && error.message ? error.message : String(error));
  }
})();
</script>
</body></html>""".replace("__SOURCE_LITERAL__", jsString(source))
    }

    private fun jsString(value: String): String = buildString {
        append('"')
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '<' -> append("\\u003c")
                '>' -> append("\\u003e")
                '&' -> append("\\u0026")
                '\u2028' -> append("\\u2028")
                '\u2029' -> append("\\u2029")
                else -> if (ch.code < 0x20) append("\\u%04x".format(ch.code)) else append(ch)
            }
        }
        append('"')
    }

    private fun projectRoot(workspace: File, projectName: String): File {
        require(projectName.isNotBlank()) { "ابتدا یک پروژه را انتخاب کنید." }
        val base = workspace.canonicalFile
        val root = File(base, projectName).canonicalFile
        require(root.isDirectory && root.path.startsWith(base.path + File.separator)) {
            "پوشه پروژه معتبر نیست."
        }
        return root
    }

    private fun safeLocalFile(root: File, relative: String): File {
        require(relative.isNotBlank() && !relative.startsWith("/") && !relative.contains('\\'))
        require(relative.split('/').none { it == "." || it == ".." })
        val file = File(root, relative).canonicalFile
        require(file.path.startsWith(root.path + File.separator)) { "مسیر فایل از محدوده پروژه خارج شده است." }
        return file
    }
}
