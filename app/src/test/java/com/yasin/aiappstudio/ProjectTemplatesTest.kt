package com.yasin.aiappstudio

import org.junit.Assert.*
import java.io.File
import java.nio.file.Files
import org.junit.Test

class ProjectTemplatesTest {
    @Test fun androidTemplateContainsCompleteComposeGradleProject() {
        val files = ProjectTemplates.files("android-kotlin", "My App")
        assertTrue(files.containsKey("settings.gradle.kts"))
        assertTrue(files.containsKey("app/build.gradle.kts"))
        assertTrue(files.containsKey(".github/workflows/android.yml"))
        assertTrue(files.getValue(".github/workflows/android.yml").contains("assembleDebug"))
        assertTrue(files.getValue(".github/workflows/android.yml").contains("bundleRelease"))
        assertTrue(files.getValue(".github/workflows/android.yml").contains("ANDROID_KEYSTORE_BASE64"))
        assertTrue(files.getValue("app/build.gradle.kts").contains("signingConfigs"))
        assertTrue(files.containsKey("app/src/main/AndroidManifest.xml"))
        val main = files.entries.first { it.key.endsWith("/MainActivity.kt") }
        assertTrue(main.value.contains("ComponentActivity"))
        assertTrue(main.value.contains("setContent"))
        assertTrue(main.value.contains("GeneratedAppTheme"))
        assertTrue(files.values.any { it.contains("com.yasin.generated.app_my_app") })
        assertTrue(files.containsKey("app/src/test/java/com/yasin/generated/app_my_app/ExampleUnitTest.kt"))
    }

    @Test fun staticWebsiteContainsResponsiveAccessibleAndSearchReadyAssets() {
        val files = ProjectTemplates.files("web-static", "My Website")
        assertTrue(files.containsKey("index.html"))
        assertTrue(files.containsKey("assets/css/styles.css"))
        assertTrue(files.containsKey("assets/js/app.js"))
        assertTrue(files.containsKey("404.html"))
        assertTrue(files.containsKey("robots.txt"))
        assertTrue(files.containsKey("sitemap.xml"))
        assertTrue(files.getValue("index.html").contains("lang=\"fa\" dir=\"rtl\""))
        assertTrue(files.getValue("assets/css/styles.css").contains("@media"))
        assertTrue(files.getValue("assets/js/app.js").contains("reportValidity"))
    }

    @Test fun reactTemplateHasPinnedDependenciesAndBuildScripts() {
        val files = ProjectTemplates.files("web-react", "React Site")
        val packageJson = files.getValue("package.json")
        assertTrue(packageJson.contains("\"build\": \"vite build\""))
        assertTrue(packageJson.contains("\"react\": \"19.0.0\""))
        assertTrue(files.containsKey("vite.config.js"))
        assertTrue(files.containsKey("src/App.jsx"))
        assertTrue(files.containsKey("src/main.jsx"))
    }

    @Test fun generatedNamesAreSafeForAndroidPackageAndWebSlug() {
        val android = ProjectTemplates.files("android-kotlin", "پروژه جدید!")
        assertTrue(android.keys.any { it.contains("com/yasin/generated/app_new_project") && it.endsWith("MainActivity.kt") })
        val web = ProjectTemplates.files("web-react", "پروژه جدید!")
        assertTrue(web.getValue("package.json").contains("\"name\": \"new-project\""))
    }
}
