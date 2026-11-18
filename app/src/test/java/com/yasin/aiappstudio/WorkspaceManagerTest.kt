package com.yasin.aiappstudio

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WorkspaceManagerTest {
    private lateinit var temp: File
    private lateinit var manager: WorkspaceManager

    @Before fun setUp() {
        temp = Files.createTempDirectory("yasin-studio-test").toFile()
        manager = WorkspaceManager(File(temp, "workspace"))
    }

    @After fun tearDown() {
        temp.deleteRecursively()
    }

    @Test fun createsRealWebTemplateAndSearchesFiles() {
        manager.createProject("نمونه وب", "web-static")
        assertTrue(File(temp, "workspace/نمونه وب/index.html").isFile)
        assertTrue(manager.listEntries("نمونه وب").any { it.path == "style.css" })
        manager.saveFile("نمونه وب", "app.js", "const message = 'hello';")
        assertEquals(1, manager.search("نمونه وب", "hello").size)
    }

    @Test fun savesSnapshotsAndSupportsProjectOperations() {
        manager.createProject("alpha")
        manager.saveFile("alpha", "src/Main.kt", "first")
        manager.saveFile("alpha", "src/Main.kt", "second")
        assertEquals("second", manager.readFile("alpha", "src/Main.kt"))
        assertEquals(1, File(temp, "workspace/alpha/.history").listFiles()!!.size)

        manager.duplicateProject("alpha", "alpha-copy")
        assertEquals("second", manager.readFile("alpha-copy", "src/Main.kt"))
        manager.renameProject("alpha-copy", "renamed")
        assertTrue(manager.listProjects().any { it.name == "renamed" })
        manager.deleteProject("renamed")
        assertFalse(File(temp, "workspace/renamed").exists())
    }

    @Test fun rejectsPathTraversalAndZipSlip() {
        manager.createProject("safe")
        try {
            manager.saveFile("safe", "../outside.txt", "bad")
            fail("Traversal path should be rejected")
        } catch (_: IllegalArgumentException) { }

        val archive = File(temp, "attack.zip")
        ZipOutputStream(FileOutputStream(archive)).use { zip ->
            zip.putNextEntry(ZipEntry("../escaped.txt"))
            zip.write("bad".toByteArray())
            zip.closeEntry()
        }
        try {
            manager.importProject(archive, "imported")
            fail("Zip Slip archive should be rejected")
        } catch (_: IllegalArgumentException) { }
        assertFalse(File(temp, "escaped.txt").exists())
        assertFalse(File(temp, "workspace/imported").exists())
    }

    @Test fun exportsAndImportsProjectArchive() {
        manager.createProject("export-me", "web-static")
        val archive = File(temp, "project.zip")
        manager.exportProject("export-me", archive)
        manager.importProject(archive, "imported")
        assertTrue(File(temp, "workspace/imported/index.html").isFile)
    }
}
