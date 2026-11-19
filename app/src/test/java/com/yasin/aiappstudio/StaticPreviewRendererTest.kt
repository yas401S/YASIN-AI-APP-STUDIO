package com.yasin.aiappstudio

import org.junit.Assert.*
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.Rule
import java.io.File

class StaticPreviewRendererTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun preservesModuleScriptsAndLocalStylesheetForWebViewAssetServing() {
        val workspace = temporaryFolder.newFolder("workspace")
        val project = File(workspace, "site").apply { mkdirs() }
        File(project, "assets").mkdirs()
        val html = """<!doctype html><html><head><link rel="stylesheet" href="/assets/site.css"></head><body><script type="module" src="/assets/main.js"></script></body></html>"""
        File(project, "index.html").writeText(html)
        File(project, "assets/site.css").writeText("body { color: navy; }")
        File(project, "assets/main.js").writeText("import './chunk.js';")
        File(project, "assets/chunk.js").writeText("document.body.dataset.ready = 'yes';")

        val rendered = StaticPreviewRenderer.render(workspace, "site")
        val root = StaticPreviewRenderer.previewRoot(workspace, "site")

        assertEquals(html, rendered)
        assertEquals(project.canonicalFile, root)
        assertNotNull(PreviewAssetResolver.resolveFile(root, "/assets/site.css"))
        assertNotNull(PreviewAssetResolver.resolveFile(root, "/assets/main.js"))
        assertNotNull(PreviewAssetResolver.resolveFile(root, "/assets/chunk.js"))
    }

    @Test
    fun selectsBuiltViteDistAsPreviewRoot() {
        val workspace = temporaryFolder.newFolder("workspace")
        val project = File(workspace, "react-site").apply { mkdirs() }
        val dist = File(project, "dist").apply { mkdirs() }
        File(dist, "assets").mkdirs()
        File(dist, "index.html").writeText(
            """<script type="module" crossorigin src="/assets/index-abc.js"></script><link rel="stylesheet" href="/assets/index-abc.css">"""
        )
        File(dist, "assets/index-abc.js").writeText("console.log('built');")
        File(dist, "assets/index-abc.css").writeText("body{}")

        val html = StaticPreviewRenderer.render(workspace, "react-site")

        assertTrue(html.contains("type=\"module\""))
        assertEquals(dist.canonicalFile, StaticPreviewRenderer.previewRoot(workspace, "react-site"))
        assertNotNull(PreviewAssetResolver.resolveFile(dist, "/assets/index-abc.js"))
        assertNotNull(PreviewAssetResolver.resolveFile(dist, "/assets/index-abc.css"))
    }

    @Test
    fun rejectsTraversalAndMissingPreviewAssets() {
        val workspace = temporaryFolder.newFolder("workspace")
        val project = File(workspace, "site").apply { mkdirs() }
        val outside = File(workspace, "secret.txt").apply { writeText("secret") }
        File(project, "index.html").writeText("<h1>Site</h1>")

        assertNull(PreviewAssetResolver.resolveFile(project, "/../secret.txt"))
        assertNull(PreviewAssetResolver.resolveFile(project, "/missing.js"))
        assertTrue(outside.isFile)
    }

    @Test
    fun mapsCommonWebAssetMimeTypes() {
        assertEquals("text/javascript", PreviewAssetResolver.mimeType(File("bundle.mjs")))
        assertEquals("text/css", PreviewAssetResolver.mimeType(File("style.css")))
        assertEquals("application/wasm", PreviewAssetResolver.mimeType(File("engine.wasm")))
        assertEquals("font/woff2", PreviewAssetResolver.mimeType(File("font.woff2")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsProjectNameOutsideWorkspace() {
        val workspace = temporaryFolder.newFolder("workspace")
        StaticPreviewRenderer.render(workspace, "../outside")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOversizedHtmlEntryPoint() {
        val workspace = temporaryFolder.newFolder("workspace")
        val project = File(workspace, "large").apply { mkdirs() }
        File(project, "index.html").writeText("x".repeat(1024 * 1024 + 1))
        StaticPreviewRenderer.render(workspace, "large")
    }
}
