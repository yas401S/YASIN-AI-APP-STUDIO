package com.yasin.aiappstudio

/** Offline starter catalog. Each template creates real editable project files. */
object ProjectTemplates {
    data class Template(val id: String, val title: String, val platform: String, val description: String)

    val catalog = listOf(
        Template("blank", "پروژه خالی", "عمومی", "ساختار ساده برای شروع آزاد"),
        Template("web-static", "وب‌سایت ساده", "وب", "HTML و CSS و JavaScript"),
        Template("web-react", "React پایه", "وب", "قالب اولیه React با Vite"),
        Template("android-kotlin", "اندروید Kotlin", "اندروید", "ساختار اولیه اپلیکیشن Kotlin و Gradle"),
        Template("godot", "بازی Godot 2D", "بازی", "پروژه پایه Godot با صحنه اصلی")
    )

    fun files(templateId: String, projectName: String): Map<String, String> {
        val safeTitle = projectName.replace("\\", "\\\\").replace("\"", "\\\"")
        return when (templateId) {
            "blank" -> mapOf("README.md" to "# $projectName\n\nساخته‌شده با YASIN AI APP STUDIO.\n")
            "web-static" -> mapOf(
                "README.md" to "# $projectName\n\nوب‌سایت ایستا. فایل index.html را در مرورگر باز کنید.\n",
                "index.html" to """<!doctype html>
<html lang="fa" dir="rtl">
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>$safeTitle</title><link rel="stylesheet" href="style.css"></head>
<body><main><h1>$safeTitle</h1><p>اولین صفحه وب شما آماده و قابل ویرایش است.</p><button id="hello">آزمایش JavaScript</button><p id="result"></p></main><script src="app.js"></script></body></html>
""",
                "style.css" to "body{font-family:system-ui,sans-serif;margin:0;background:#f3f6fb;color:#17243a}main{max-width:760px;margin:12vh auto;padding:2rem;background:white;border-radius:18px;box-shadow:0 8px 32px #15243a18}button{padding:.7rem 1rem;border:0;border-radius:8px;background:#2563eb;color:white}\n",
                "app.js" to "document.querySelector('#hello').addEventListener('click',()=>{document.querySelector('#result').textContent='JavaScript اجرا شد.'});\n"
            )
            "web-react" -> mapOf(
                "README.md" to "# $projectName\n\nقالب React + Vite. برای اجرا به Node.js و نصب وابستگی‌ها نیاز دارد.\n",
                "package.json" to """{"name":"${projectName.lowercase().replace(Regex("[^a-z0-9-]"), "-")}","version":"1.0.0","private":true,"type":"module","scripts":{"dev":"vite","build":"vite build","preview":"vite preview"},"dependencies":{"@vitejs/plugin-react":"latest","vite":"latest","react":"latest","react-dom":"latest"},"devDependencies":{}}""",
                "index.html" to "<div id=\"root\"></div><script type=\"module\" src=\"/src/main.jsx\"></script>\n",
                "src/main.jsx" to "import React from 'react';\nimport { createRoot } from 'react-dom/client';\nimport './style.css';\nfunction App(){return <main><h1>$safeTitle</h1><p>اولین پروژه React شما</p></main>}\ncreateRoot(document.getElementById('root')).render(<App/>);\n",
                "src/style.css" to "body{font-family:system-ui;margin:0;padding:2rem;background:#f4f6fb;color:#17243a}\n"
            )
            "android-kotlin" -> mapOf(
                "README.md" to "# $projectName\n\nپروژه Android/Kotlin پایه. برای ساخت APK به Android SDK و Gradle نیاز است.\n",
                "settings.gradle.kts" to "pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\ndependencyResolutionManagement { repositories { google(); mavenCentral() } }\nrootProject.name = \"$safeTitle\"\ninclude(\":app\")\n",
                "build.gradle.kts" to "plugins { id(\"com.android.application\") version \"8.9.1\" apply false; id(\"org.jetbrains.kotlin.android\") version \"2.0.21\" apply false }\n",
                "app/build.gradle.kts" to "plugins { id(\"com.android.application\"); id(\"org.jetbrains.kotlin.android\") }\nandroid { namespace = \"com.example.app\"; compileSdk = 35; defaultConfig { applicationId = \"com.example.app\"; minSdk = 26; targetSdk = 35 } }\n",
                "app/src/main/AndroidManifest.xml" to "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"><application android:label=\"$safeTitle\"><activity android:name=\".MainActivity\" android:exported=\"true\"><intent-filter><action android:name=\"android.intent.action.MAIN\"/><category android:name=\"android.intent.category.LAUNCHER\"/></intent-filter></activity></application></manifest>\n",
                "app/src/main/java/com/example/app/MainActivity.kt" to "package com.example.app\n\nimport android.app.Activity\nimport android.os.Bundle\nimport android.widget.TextView\n\nclass MainActivity : Activity() { override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContentView(TextView(this).apply { text = \"$safeTitle\"; textSize = 24f }) } }\n"
            )
            "godot" -> mapOf(
                "README.md" to "# $projectName\n\nپروژه Godot 2D پایه. اجرای بازی به نصب Godot نیاز دارد.\n",
                "project.godot" to """config_version=5
[application]
config/name="$safeTitle"
run/main_scene="res://Main.tscn"
[rendering]
renderer/rendering_method="gl_compatibility"
""",
                "Main.tscn" to """[gd_scene load_steps=2 format=3]
[ext_resource type="Script" path="res://Main.gd" id="1"]
[node name="Main" type="Node2D"]
script = ExtResource("1")
""",
                "Main.gd" to "extends Node2D\n\nfunc _ready():\n    print(\"$safeTitle is ready\")\n"
            )
            else -> error("قالب انتخاب‌شده پشتیبانی نمی‌شود.")
        }
    }
}
