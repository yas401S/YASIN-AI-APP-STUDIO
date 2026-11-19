package com.yasin.aiappstudio

import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioProjectContextBuilderTest {
    @Test
    fun prioritizesBuildConfigurationAndEntryPoints() {
        val root = Files.createTempDirectory("studio-context").toFile()
        try {
            root.resolve("README.md").writeText("readme")
            root.resolve("app/src/main/java/MainActivity.kt").apply {
                parentFile.mkdirs()
                writeText("activity")
            }
            root.resolve("build.gradle.kts").writeText("root-build")
            root.resolve("settings.gradle.kts").writeText("settings")
            val context = StudioProjectContextBuilder.build(root)
            assertTrue(context.indexOf("FILE: settings.gradle.kts") < context.indexOf("FILE: build.gradle.kts"))
            assertTrue(context.indexOf("FILE: build.gradle.kts") < context.indexOf("FILE: app/src/main/java/MainActivity.kt"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun excludesGeneratedAndHiddenDirectories() {
        val root = Files.createTempDirectory("studio-context").toFile()
        try {
            root.resolve("src/main.kt").apply { parentFile.mkdirs(); writeText("keep") }
            root.resolve("build/generated.kt").apply { parentFile.mkdirs(); writeText("exclude") }
            root.resolve(".git/config.kt").apply { parentFile.mkdirs(); writeText("exclude") }
            val context = StudioProjectContextBuilder.build(root)
            assertTrue(context.contains("src/main.kt"))
            assertFalse(context.contains("build/generated.kt"))
            assertFalse(context.contains(".git/config.kt"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun respectsMaximumContextSize() {
        val root = Files.createTempDirectory("studio-context").toFile()
        try {
            root.resolve("one.kt").writeText("x".repeat(500))
            val context = StudioProjectContextBuilder.build(root, maxCharacters = 80)
            assertTrue(context.length <= 80)
        } finally {
            root.deleteRecursively()
        }
    }
}
