package com.yasin.aiappstudio

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.Rule
import java.io.File

class StaticPreviewRendererTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun inlinesLocalCssAndJavascriptIntoPreview() {
        val workspace = temporaryFolder.newFolder("workspace")
        val project = File(workspace, "site").apply { mkdirs() }
        File(project, "index.html").writeText(
            """<!doctype html><html><head><link rel="stylesheet" href="style.css"></head><body><h1>Hi</h1><script src="app.js"></script></body></html>"""
        )
        File(project, "style.css").writeText("body { color: navy; }")
        File(project, "app.js").writeText("document.body.dataset.ready = 'yes';")

        val html = StaticPreviewRenderer.render(workspace, "site")

        assertTrue(html.contains("<style>"))
        assertTrue(html.contains("color: navy"))
        assertTrue(html.contains("<script>"))
        assertTrue(html.contains("dataset.ready"))
        assertTrue(!html.contains("href=\"style.css\""))
        assertTrue(!html.contains("src=\"app.js\""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTraversalInLocalAssetPath() {
        val workspace = temporaryFolder.newFolder("workspace")
        val project = File(workspace, "site").apply { mkdirs() }
        File(project, "index.html").writeText("<link rel=\"stylesheet\" href=\"../outside.css\">")
        StaticPreviewRenderer.render(workspace, "site")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsRemoteAssets() {
        val workspace = temporaryFolder.newFolder("workspace")
        val project = File(workspace, "site").apply { mkdirs() }
        File(project, "index.html").writeText("<script src=\"https://example.com/app.js\"></script>")
        StaticPreviewRenderer.render(workspace, "site")
    }
}
