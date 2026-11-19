package com.yasin.aiappstudio

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class FileHistoryTest {
    @Test fun listsNewestSnapshotsForExactFileAndRestoresWithoutLosingCurrentVersion() {
        val root = Files.createTempDirectory("studio-history").toFile()
        try {
            val manager = WorkspaceManager(File(root, "workspace"))
            manager.createProject("demo")
            manager.saveFile("demo", "src/Main.kt", "version zero")
            manager.saveFile("demo", "src/Main.kt", "version one")
            Thread.sleep(2)
            manager.saveFile("demo", "src/Main.kt", "version two")

            val history = FileHistory.list(File(root, "workspace"), "demo", "src/Main.kt")
            assertEquals(2, history.size)
            assertTrue(history[0].timestamp >= history[1].timestamp)

            manager.saveFile("demo", "src/Main.kt", history.first().file.readText())
            assertEquals("version one", manager.readFile("demo", "src/Main.kt"))
            assertEquals(3, FileHistory.list(File(root, "workspace"), "demo", "src/Main.kt").size)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun rejectsTraversalAndDoesNotMixDifferentFiles() {
        val root = Files.createTempDirectory("studio-history-safe").toFile()
        try {
            val workspace = File(root, "workspace")
            val manager = WorkspaceManager(workspace)
            manager.createProject("demo")
            manager.saveFile("demo", "a_b.txt", "underscore zero")
            manager.saveFile("demo", "a_b.txt", "underscore")
            manager.saveFile("demo", "a/b.txt", "nested zero")
            manager.saveFile("demo", "a/b.txt", "nested")
            assertEquals(1, FileHistory.list(workspace, "demo", "a_b.txt").size)
            assertEquals(1, FileHistory.list(workspace, "demo", "a/b.txt").size)
            try {
                FileHistory.list(workspace, "demo", "../outside")
                fail("Traversal must be rejected")
            } catch (_: IllegalArgumentException) { }
        } finally {
            root.deleteRecursively()
        }
    }
}
