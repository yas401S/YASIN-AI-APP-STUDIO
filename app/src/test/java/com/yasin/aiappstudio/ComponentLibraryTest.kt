package com.yasin.aiappstudio

import org.junit.Assert.*
import org.junit.Test

class ComponentLibraryTest {
    @Test fun catalogContainsRealEditableSnippetsAcrossPlatforms() {
        assertTrue(ComponentLibrary.snippets.size >= 10)
        assertTrue(ComponentLibrary.snippets.any { it.language == "HTML" })
        assertTrue(ComponentLibrary.snippets.any { it.language == "React" })
        assertTrue(ComponentLibrary.snippets.any { it.language == "Kotlin" })
        assertTrue(ComponentLibrary.snippets.any { it.language == "GDScript" })
        assertTrue(ComponentLibrary.snippets.all { it.code.isNotBlank() })
    }

    @Test fun searchFiltersByTitleDescriptionAndLanguage() {
        assertTrue(ComponentLibrary.search("React").any { it.id == "react-component" })
        assertTrue(ComponentLibrary.search("Compose").any { it.id == "kotlin-compose" })
        assertTrue(ComponentLibrary.search("", "CSS").all { it.language == "CSS" })
        assertTrue(ComponentLibrary.search("not-a-real-snippet").isEmpty())
    }

    @Test fun allStarterTemplatesAreSearchableByExistingMetadata() {
        assertEquals(5, ProjectTemplates.catalog.size)
        assertTrue(ProjectTemplates.catalog.any { it.title.contains("Godot") })
        assertTrue(ProjectTemplates.catalog.any { it.platform == "اندروید" })
    }
}
