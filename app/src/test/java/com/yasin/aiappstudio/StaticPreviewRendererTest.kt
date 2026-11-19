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

    @Test
    fun embedsLocalImagesAndCssBackgroundAssets() {
        val workspace = temporaryFolder.newFolder("workspace")
        val project = File(workspace, "site").apply { mkdirs() }
        File(project, "assets").mkdirs()
        File(project, "index.html").writeText(
            """<html><head><link rel="stylesheet" href="assets/site.css"></head><body><img src="assets/logo.png"></body></html>"""
        )
        File(project, "assets/site.css").writeText("body { background-image: url('background.png'); }")
        File(project, "assets/logo.png").writeBytes(byteArrayOf(1, 2, 3))
        File(project, "assets/background.png").writeBytes(byteArrayOf(4, 5, 6))

        val html = StaticPreviewRenderer.render(workspace, "site")

        assertTrue(html.contains("data:image/png;base64,"))
        assertTrue(!html.contains("src=\"assets/logo.png\""))
        assertTrue(!html.contains("url('background.png')"))
    }

    @Test
    fun previewsBuiltViteDistDirectory() {
        val workspace = temporaryFolder.newFolder("workspace")
        val project = File(workspace, "react-site").apply { mkdirs() }
        val dist = File(project, "dist").apply { mkdirs() }
        File(dist, "index.html").writeText("<html><body><h1>Built app</h1></body></html>")

        val html = StaticPreviewRenderer.render(workspace, "react-site")

        assertTrue(html.contains("Built app"))
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
