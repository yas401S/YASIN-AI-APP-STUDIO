package com.yasin.aiappstudio

import android.os.Bundle
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF081426)
private val Panel = Color(0xFF11233B)
private val Blue = Color(0xFF3286F6)
private val Gold = Color(0xFFFFC857)
private val Muted = Color(0xFF9DB0C9)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Blue, secondary = Gold, background = Navy, surface = Panel)) {
                StudioApp()
            }
        }
    }
}

private data class WorkspaceFile(val file: File, val relative: String, val directory: Boolean)
private data class StudioChatMessage(val role: String, val content: String)
private data class StudioFilePatch(val path: String, val content: String)

@Composable
private fun StudioApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("studio", 0) }
    val workspace = remember { File(context.filesDir, "workspace").apply { mkdirs() } }
    var page by remember { mutableStateOf("خانه") }
    val projects = remember {
        mutableStateListOf<String>().apply {
            try {
                val saved = JSONArray(prefs.getString("projects", "[]"))
                for (i in 0 until saved.length()) add(saved.getString(i))
            } catch (_: Exception) { }
        }
    }
    var selectedProject by remember { mutableStateOf("") }
    var selectedPath by remember { mutableStateOf("README.md") }
    var editorText by remember { mutableStateOf("") }
    var diagnosticMessage by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var fileItems by remember { mutableStateOf(listOf<WorkspaceFile>()) }
    var draftName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf("blank") }
    var templateSearch by remember { mutableStateOf("") }
    var showSnippets by remember { mutableStateOf(false) }
    var snippetSearch by remember { mutableStateOf("") }
    var previewHtml by remember { mutableStateOf("") }
    var previewUsesReact by remember { mutableStateOf(false) }
    var previewError by remember { mutableStateOf("") }
    var createType by remember { mutableStateOf("file") }
    var showCreateProject by remember { mutableStateOf(false) }
    var showCreateEntry by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showRenameProject by remember { mutableStateOf(false) }
    var showDeleteProject by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var historyItems by remember { mutableStateOf(listOf<FileSnapshot>()) }
    var projectToManage by remember { mutableStateOf("") }
    var projectNameDraft by remember { mutableStateOf("") }
    var renameText by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf("") }
    val chatMessages = remember {
        mutableStateListOf<StudioChatMessage>().apply {
            runCatching {
                val saved = JSONArray(prefs.getString("studio_chat", "[]"))
                for (i in 0 until saved.length()) {
                    val item = saved.getJSONObject(i)
                    val role = item.optString("role")
                    val body = item.optString("content")
                    if ((role == "user" || role == "assistant") && body.isNotBlank()) add(StudioChatMessage(role, body))
                }
            }
        }
    }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageName by remember { mutableStateOf("") }
    var pendingAiPatches by remember { mutableStateOf<List<StudioFilePatch>>(emptyList()) }
    var pendingAiSummary by remember { mutableStateOf("") }
    var showAiPatchPreview by remember { mutableStateOf(false) }
    var usageSummary by remember { mutableStateOf("توکن ثبت‌شده: ورودی " + prefs.getLong("usage_prompt", 0L) + "، خروجی " + prefs.getLong("usage_completion", 0L) + "، مجموع " + prefs.getLong("usage_total", 0L)) }
    var endpoint by remember { mutableStateOf("https://yasin-ai-app-studio-ai.yasinjon-401.workers.dev/v1/chat/completions") }
    var model by remember { mutableStateOf("@cf/meta/llama-3.2-11b-vision-instruct") }
    var apiKey by remember { mutableStateOf(SecureSecretStore.read(context)) }
    var githubToken by remember { mutableStateOf(GitHubTokenStore.read(context)) }
    var githubDeviceCode by remember { mutableStateOf<GitHubDeviceCode?>(null) }
    var githubStatus by remember { mutableStateOf(if (githubToken.isNotBlank()) "حساب GitHub متصل است." else "هنوز حساب GitHub متصل نشده است.") }
    var githubBusy by remember { mutableStateOf(false) }
    var buildBranch by remember { mutableStateOf("") }
    var buildStatus by remember { mutableStateOf("هنوز ساختی اجرا نشده است.") }
    var buildRunUrl by remember { mutableStateOf("") }
    var buildBusy by remember { mutableStateOf(false) }
    var webBuildBranch by remember { mutableStateOf("") }
    var webBuildProjectSlug by remember { mutableStateOf("") }
    var webBuildProjectName by remember { mutableStateOf("") }
    var webBuildArtifactId by remember { mutableStateOf<Long?>(null) }
    var cloudflarePreviewUrl by remember { mutableStateOf("") }
    var webBuildStatus by remember { mutableStateOf("هنوز ساخت وب اجرا نشده است.") }
    var webBuildRunUrl by remember { mutableStateOf("") }
    var webBuildBusy by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var visionStage by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val aiImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
        selectedImageName = uri?.lastPathSegment?.substringAfterLast('/')?.take(60).orEmpty()
    }
    fun persistAiChat() {
        val array = JSONArray()
        chatMessages.takeLast(50).forEach { array.put(JSONObject().put("role", it.role).put("content", it.content)) }
        prefs.edit().putString("studio_chat", array.toString()).apply()
    }
    fun projectDir(): File = File(workspace, selectedProject)
    fun refreshFiles() {
        val root = projectDir()
        fileItems = if (root.exists()) root.walkTopDown()
            .filter { it != root && !it.relativeTo(root).path.split(File.separator).any { p -> p.startsWith(".") } }
            .map { WorkspaceFile(it, it.relativeTo(root).invariantSeparatorsPath, it.isDirectory) }
            .sortedWith(compareBy<WorkspaceFile>({ !it.directory }, { it.relative.lowercase() })).toList()
        else emptyList()
    }
    fun loadProjectPreview() {
        val projectRoot = File(workspace, selectedProject)
        val hasBuiltWebOutput = File(projectRoot, "dist/index.html").isFile
        previewUsesReact = !hasBuiltWebOutput && ReactPreviewRenderer.isReactProject(workspace, selectedProject)
        previewHtml = if (previewUsesReact) {
            ReactPreviewRenderer.render(workspace, selectedProject)
        } else {
            StaticPreviewRenderer.render(workspace, selectedProject)
        }
        previewError = ""
    }
    fun openFile(relative: String) {
        runCatching {
            selectedPath = relative
            editorText = WorkspaceManager(workspace).readFile(selectedProject, relative)
            page = "ویرایشگر"
        }.onFailure { notice = it.message ?: "بازکردن فایل ناموفق بود." }
    }
    fun saveFile() {
        runCatching {
            WorkspaceManager(workspace).saveFile(selectedProject, selectedPath, editorText)
            notice = "فایل ذخیره شد و نسخه قبلی در تاریخچه ثبت شد."
            refreshFiles()
        }.onFailure { notice = "ذخیره فایل ناموفق بود: " + (it.message ?: "خطا") }
    }
    fun persistProjects() { prefs.edit().putString("projects", JSONArray(projects).toString()).apply() }
    val exportProjectLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && selectedProject.isNotBlank()) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val archive = File(context.cacheDir, "studio-export-${System.currentTimeMillis()}.zip")
                    try {
                        WorkspaceManager(workspace).exportProject(selectedProject, archive)
                        val output = context.contentResolver.openOutputStream(uri) ?: error("ذخیره فایل انتخاب‌شده ممکن نیست.")
                        output.use { stream -> archive.inputStream().use { it.copyTo(stream) } }
                    } finally { archive.delete() }
                }
            }.onSuccess { notice = "فایل ZIP پروژه صادر شد." }
             .onFailure { notice = it.message ?: "خروجی گرفتن ناموفق بود." }
        }
    }
    val importProjectLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val archive = File(context.cacheDir, "studio-import-${System.currentTimeMillis()}.zip")
                    try {
                        val input = context.contentResolver.openInputStream(uri) ?: error("خواندن فایل انتخاب‌شده ممکن نیست.")
                        input.use { source -> archive.outputStream().use { source.copyTo(it) } }
                        val importedName = "Imported-${System.currentTimeMillis()}"
                        WorkspaceManager(workspace).importProject(archive, importedName)
                        importedName
                    } finally { archive.delete() }
                }
            }.onSuccess { importedName ->
                projects.add(0, importedName); persistProjects()
                selectedProject = importedName; refreshFiles()
                notice = "پروژه وارد شد: $importedName"
            }.onFailure { notice = it.message ?: "ورود پروژه ناموفق بود." }
        }
    }

    Surface(Modifier.fillMaxSize(), color = Navy) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().background(Panel).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = Gold, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("YASIN AI APP STUDIO", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text("محیط توسعه نرم‌افزار", color = Muted, fontSize = 11.sp)
                }
                if (selectedProject.isNotBlank()) Text(selectedProject, color = Gold, fontSize = 11.sp)
            }
            when (page) {
                "خانه" -> Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("از ایده تا نرم‌افزار", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("پروژه بساز، فایل‌ها را مدیریت کن و با هوش مصنوعی توسعه بده.", color = Muted)
                    Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp)) {
                            Text("پروژه‌های شما", color = Gold)
                            Text(projects.size.toString(), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Button(onClick = { showCreateProject = true }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("ساخت پروژه جدید")
                            }
                        }
                    }
                    listOf("پروژه‌ها" to "مدیریت پروژه‌ها و پوشه‌ها", "ویرایشگر" to "مدیریت فایل و کد", "هوش مصنوعی" to "دستیار برنامه‌نویسی", "تنظیمات" to "تنظیم اتصال مدل").forEach { item ->
                        Card(Modifier.fillMaxWidth().clickable { page = item.first }, colors = CardDefaults.cardColors(containerColor = Panel)) {
                            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(item.first, color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Text(item.second, color = Muted, fontSize = 11.sp)
                                Icon(Icons.Default.ChevronRight, null, tint = Gold)
                            }
                        }
                    }
                }
                "پروژه‌ها" -> Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("پروژه‌ها و فایل‌ها", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { importProjectLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed")) }) { Text("ورود ZIP") }
                        TextButton(onClick = { if (selectedProject.isNotBlank()) exportProjectLauncher.launch("$selectedProject.zip") else notice = "ابتدا یک پروژه را باز کنید." }) { Text("خروجی ZIP") }
                        Button(onClick = { showCreateProject = true }) { Icon(Icons.Default.Add, null); Text("پروژه") }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (selectedProject.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("پروژه فعال: $selectedProject", color = Gold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { showCreateEntry = true }) { Text("فایل/پوشه جدید") }
                            TextButton(onClick = {
                                runCatching { loadProjectPreview(); page = "پیش‌نمایش" }
                                    .onFailure { previewError = it.message ?: "ساخت پیش‌نمایش ناموفق بود."; notice = previewError }
                            }) { Text("پیش‌نمایش وب") }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(searchQuery, { searchQuery = it }, modifier = Modifier.weight(1f),
                                singleLine = true, label = { Text("جست‌وجو در فایل‌های پروژه") })
                            Button(onClick = {
                                runCatching { searchResults = WorkspaceManager(workspace).search(selectedProject, searchQuery) }
                                    .onFailure { notice = it.message ?: "جست‌وجو ناموفق بود." }
                            }) { Text("بگرد") }
                        }
                        if (searchResults.isNotEmpty()) {
                            Text("نتایج جست‌وجو", color = Gold, fontWeight = FontWeight.Bold)
                            searchResults.forEach { hit ->
                                TextButton(onClick = {
                                    val path = hit.first.substringBeforeLast(":")
                                    openFile(path)
                                }) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Text(hit.first, color = Gold, fontSize = 11.sp)
                                        Text(hit.second, color = Muted, fontSize = 11.sp, maxLines = 2)
                                    }
                                }
                            }
                        }
                        TextButton(onClick = { refreshFiles() }) { Text("بازخوانی فهرست فایل‌ها") }
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(projects, key = { it }) { name ->
                            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Folder, null, tint = Gold)
                                    Spacer(Modifier.width(8.dp))
                                    Text(name, color = Color.White, modifier = Modifier.weight(1f))
                                    TextButton(onClick = {
                                        selectedProject = name
                                        File(workspace, name).mkdirs()
                                        refreshFiles()
                                    }) { Text(if (selectedProject == name) "فعال" else "بازکردن") }
                                    TextButton(onClick = {
                                        runCatching {
                                            val copyName = "${name}-copy-${System.currentTimeMillis() % 10000}"
                                            WorkspaceManager(workspace).duplicateProject(name, copyName)
                                            projects.add(0, copyName); persistProjects(); notice = "پروژه تکثیر شد."
                                        }.onFailure { notice = it.message ?: "تکثیر ناموفق بود." }
                                    }) { Text("کپی") }
                                    IconButton(onClick = { projectToManage = name; projectNameDraft = name; showRenameProject = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "تغییر نام پروژه", tint = Muted)
                                    }
                                    IconButton(onClick = { projectToManage = name; showDeleteProject = true }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف پروژه", tint = Color(0xFFFF7777))
                                    }
                                }
                                if (selectedProject == name) {
                                    fileItems.forEach { entry ->
                                        Row(Modifier.fillMaxWidth().clickable {
                                            if (entry.directory) notice = "پوشه: " + entry.relative else openFile(entry.relative)
                                        }.padding(start = 24.dp, end = 8.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(if (entry.directory) Icons.Default.Folder else Icons.Default.Description, null, tint = Muted, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(entry.relative, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            if (!entry.directory) TextButton(onClick = { openFile(entry.relative) }) { Text("ویرایش") }
                                            IconButton(onClick = { renameText = entry.file.name; selectedPath = entry.relative; showRename = true }) { Icon(Icons.Default.Edit, null, tint = Muted) }
                                            IconButton(onClick = {
                                                runCatching { WorkspaceManager(workspace).deleteEntry(selectedProject, entry.relative); refreshFiles(); notice = "حذف شد." }
                                                    .onFailure { notice = "حذف ناموفق بود." }
                                            }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF7777)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (notice.isNotBlank()) Text(notice, color = Gold, fontSize = 12.sp)
                }
                "پیش‌نمایش" -> Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (previewUsesReact) "پیش‌نمایش React" else "پیش‌نمایش وب ایستا", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { runCatching { loadProjectPreview() }.onFailure { previewError = it.message ?: "خطا" } }) { Text("بازخوانی") }
                    }
                    Text(if (previewUsesReact) "پیش‌نمایش سریع React تک‌فایلی. برای پروژه چندفایلی: کد را به GitHub بفرستید، workflow با نام React Web Build را اجرا کنید، فایل react-web-dist را دانلود و ZIP آن را به‌عنوان پروژه وارد کنید؛ سپس خروجی ساخته‌شده به‌صورت کامل پیش‌نمایش می‌شود." else "فایل‌های HTML، CSS، JavaScript، ماژول‌ها و دارایی‌های محلی پروژه از فضای محدودشده همان پروژه بارگذاری می‌شوند؛ درخواست شبکه خارجی مسدود است.", color = Muted, fontSize = 11.sp)
                    if (previewError.isNotBlank()) Text(previewError, color = Color(0xFFFF8888), fontSize = 12.sp)
                    key(selectedProject, previewHtml, previewUsesReact) {
                        AndroidView(modifier = Modifier.fillMaxSize().weight(1f), factory = { ctx ->
                            val projectRoot = if (previewUsesReact) File(workspace, selectedProject).canonicalFile
                                else StaticPreviewRenderer.previewRoot(workspace, selectedProject)
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                // All requests are intercepted: project files are served from this root,
                                // and React may additionally fetch only its three pinned CDN scripts.
                                settings.blockNetworkLoads = false
                                settings.domStorageEnabled = false
                                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                webViewClient = object : WebViewClient() {
                                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                        val uri = request?.url ?: return WebResourceResponse(
                                            "text/plain", "UTF-8", 403, "Blocked", emptyMap(),
                                            ByteArrayInputStream(ByteArray(0))
                                        )
                                        if (uri.host == "studio-preview.invalid") {
                                            if (request.method != "GET") return WebResourceResponse(
                                                "text/plain", "UTF-8", 403, "Blocked", emptyMap(),
                                                ByteArrayInputStream(ByteArray(0))
                                            )
                                            val asset = PreviewAssetResolver.resolveFile(projectRoot, uri.path ?: "/")
                                                ?: return WebResourceResponse(
                                                    "text/plain", "UTF-8", 404, "Not Found", emptyMap(),
                                                    ByteArrayInputStream(ByteArray(0))
                                                )
                                            val mime = PreviewAssetResolver.mimeType(asset)
                                            return WebResourceResponse(mime, PreviewAssetResolver.charsetFor(mime), FileInputStream(asset))
                                        }
                                        if (previewUsesReact && ReactPreviewRenderer.isAllowedAsset(uri.toString())) {
                                            return super.shouldInterceptRequest(view, request)
                                        }
                                        return WebResourceResponse(
                                            "text/plain", "UTF-8", 403, "Blocked", emptyMap(),
                                            ByteArrayInputStream(ByteArray(0))
                                        )
                                    }
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
                                }
                                loadDataWithBaseURL("https://studio-preview.invalid/", previewHtml, "text/html", "UTF-8", null)
                            }
                        }, update = { view -> view.loadDataWithBaseURL("https://studio-preview.invalid/", previewHtml, "text/html", "UTF-8", null) })
                    }
                }
                "ویرایشگر" -> Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("ویرایشگر کد", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Text("$selectedProject / $selectedPath", color = Muted, fontSize = 11.sp)
                        }
                        TextButton(onClick = {
                            runCatching { historyItems = FileHistory.list(workspace, selectedProject, selectedPath); showHistory = true }
                                .onFailure { notice = it.message ?: "خواندن تاریخچه ناموفق بود." }
                        }, enabled = selectedProject.isNotBlank()) { Icon(Icons.Default.History, null); Text("تاریخچه") }
                        TextButton(onClick = { snippetSearch = ""; showSnippets = true }, enabled = selectedProject.isNotBlank()) {
                            Icon(Icons.Default.Code, null); Text("کتابخانه کد")
                        }
                        TextButton(onClick = {
                            val issues = DiagnosticEngine.inspect(selectedPath, editorText)
                            val report = DiagnosticEngine.repairSafeFormatting(editorText)
                            val safeChanges = report.fixedCount > 0 && report.content != editorText
                            if (safeChanges) {
                                runCatching {
                                    WorkspaceManager(workspace).saveFile(selectedProject, selectedPath, report.content)
                                    editorText = report.content
                                    refreshFiles()
                                }.onFailure { diagnosticMessage = "اصلاح ذخیره نشد: " + (it.message ?: "خطا") }
                            }
                            if (issues.isEmpty()) {
                                diagnosticMessage = "بررسی محلی: ایراد قابل تشخیصی پیدا نشد."
                            } else {
                                val fixed = if (safeChanges) "؛ ${report.fixedCount} اصلاح قالب‌بندی ذخیره شد" else ""
                                val details = issues.take(5).joinToString(" | ") { "خط ${it.line}: ${it.message}" }
                                val remaining = if (issues.size > 5) " | و ${issues.size - 5} مورد دیگر" else ""
                                diagnosticMessage = "${issues.size} مورد پیدا شد$fixed. $details$remaining"
                            }
                        }, enabled = selectedProject.isNotBlank()) { Icon(Icons.Default.Build, null); Text("بررسی و اصلاح امن") }
                        Button(onClick = { saveFile() }, enabled = selectedProject.isNotBlank()) { Icon(Icons.Default.Save, null); Text("ذخیره") }
                    }
                    BasicTextField(value = editorText, onValueChange = { editorText = it },
                        modifier = Modifier.fillMaxSize().weight(1f).background(Color(0xFF050D18), RoundedCornerShape(10.dp)).padding(14.dp),
                        textStyle = TextStyle(color = Color(0xFFB8D8FF), fontSize = 13.sp, lineHeight = 20.sp))
                    if (diagnosticMessage.isNotBlank()) Text(diagnosticMessage, color = Gold, fontSize = 11.sp)
                    Text("اصلاح خودکار فعلی فقط قالب‌بندی بی‌خطر را تغییر می‌دهد؛ تشخیص خطای کامپایل و تعمیر معنایی نیازمند اجرای build و تأیید تغییرات است.", color = Muted, fontSize = 10.sp)
                    if (notice.isNotBlank()) Text(notice, color = Gold, fontSize = 11.sp)
                }
                "GitHub" -> Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("اتصال GitHub", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("برای ارسال پروژه و اجرای ساخت APK، حساب GitHub خود را با Device Flow متصل کنید.", color = Muted)
                    Text("Client ID برنامه", color = Gold, fontSize = 12.sp)
                    Text("Iv23liSXdallpdwXkHz", color = Color.White, fontSize = 13.sp)
                    Text(githubStatus, color = if (githubToken.isNotBlank()) Color(0xFF70D6A0) else Gold, fontSize = 12.sp)
                    Button(onClick = {
                        githubBusy = true
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) { GitHubDeviceFlow.requestDeviceCode("Iv23liSXdallpdwXkHz") }
                            }.onSuccess { code ->
                                githubDeviceCode = code
                                githubStatus = "کد ورود آماده است. صفحه GitHub را باز کنید و کد زیر را وارد کنید."
                            }.onFailure { githubStatus = "گرفتن کد ورود ناموفق بود: " + (it.message ?: "خطای ناشناخته") }
                            githubBusy = false
                        }
                    }, enabled = !githubBusy, modifier = Modifier.fillMaxWidth()) {
                        if (githubBusy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Link, null)
                        Spacer(Modifier.width(8.dp)); Text("دریافت کد اتصال")
                    }
                    githubDeviceCode?.let { code ->
                        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("کد یک‌بارمصرف GitHub", color = Gold)
                                Text(code.userCode, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                Text("این کد را فقط در صفحه رسمی GitHub وارد کنید. اعتبار: حدود ${code.expiresInSeconds / 60} دقیقه.", color = Muted, fontSize = 11.sp)
                                Button(onClick = {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(code.verificationUri))) }
                                        .onFailure { githubStatus = "بازکردن مرورگر ممکن نشد." }
                                }, modifier = Modifier.fillMaxWidth()) { Text("بازکردن صفحه رسمی GitHub") }
                                Button(onClick = {
                                    githubBusy = true
                                    scope.launch {
                                        runCatching {
                                            withContext(Dispatchers.IO) {
                                                GitHubDeviceFlow.pollForAccessToken("Iv23liSXdallpdwXkHz", code.deviceCode)
                                            }
                                        }.onSuccess { token ->
                                            if (token.isNullOrBlank()) {
                                                githubStatus = "هنوز تأیید نشده؛ پس از واردکردن کد دوباره بررسی کن."
                                            } else {
                                                GitHubTokenStore.write(context, token)
                                                githubToken = token
                                                githubDeviceCode = null
                                                githubStatus = "اتصال GitHub موفق شد؛ توکن به‌صورت رمزگذاری‌شده در گوشی ذخیره شد."
                                            }
                                        }.onFailure { githubStatus = it.message ?: "بررسی اتصال ناموفق بود." }
                                        githubBusy = false
                                    }
                                }, enabled = !githubBusy, modifier = Modifier.fillMaxWidth()) {
                                    Text("بررسی تأیید و اتصال")
                                }
                            }
                        }
                    }
                    if (githubToken.isNotBlank()) {
                        OutlinedButton(onClick = {
                            GitHubTokenStore.write(context, "")
                            githubToken = ""
                            githubDeviceCode = null
                            githubStatus = "اتصال محلی پاک شد. برای لغو کامل دسترسی، GitHub App را از تنظیمات حساب لغو نصب کنید."
                        }, modifier = Modifier.fillMaxWidth()) { Text("قطع اتصال از این گوشی") }
                    }
                    Divider(color = Muted.copy(alpha = 0.25f))
                    Text("ساخت APK پروژه اندرویدی", color = Gold, fontWeight = FontWeight.Bold)
                    Text(if (selectedProject.isBlank()) "ابتدا از بخش پروژه‌ها یک پروژه Android Kotlin را باز کن." else "پروژه فعال: $selectedProject", color = Muted, fontSize = 12.sp)
                    Text(buildStatus, color = Gold, fontSize = 12.sp)
                    Button(onClick = {
                        if (githubToken.isBlank()) {
                            buildStatus = "ابتدا GitHub را در همین صفحه متصل کن."
                        } else if (selectedProject.isBlank()) {
                            buildStatus = "ابتدا یک پروژه را باز کن."
                        } else {
                            buildBusy = true
                            buildStatus = "در حال ارسال فایل‌های پروژه به شاخه اختصاصی GitHub..."
                            scope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        GitHubAndroidBuild.uploadProjectAndDispatch(
                                            githubToken, File(workspace, selectedProject), selectedProject
                                        )
                                    }
                                }.onSuccess { createdBranch ->
                                    buildBranch = createdBranch
                                    buildStatus = "پروژه ارسال شد و دستور ساخت اجرا شد. برای مشاهده نتیجه، وضعیت را بررسی کن."
                                }.onFailure {
                                    buildStatus = "ساخت شروع نشد: " + (it.message ?: "خطای نامشخص")
                                }
                                buildBusy = false
                            }
                        }
                    }, enabled = !buildBusy && githubToken.isNotBlank() && selectedProject.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        if (buildBusy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Build, null)
                        Spacer(Modifier.width(8.dp)); Text("ارسال پروژه و ساخت APK")
                    }
                    OutlinedButton(onClick = {
                        if (githubToken.isBlank() || buildBranch.isBlank()) {
                            buildStatus = "ابتدا پروژه را ارسال کن تا ساخت شروع شود."
                        } else {
                            buildBusy = true
                            scope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) { GitHubAndroidBuild.latestBuild(githubToken, buildBranch) }
                                }.onSuccess { run ->
                                    if (run == null) {
                                        buildStatus = "اجرای ساخت هنوز در GitHub ثبت نشده است؛ چند لحظه دیگر دوباره بررسی کن."
                                    } else {
                                        buildRunUrl = run.htmlUrl
                                        buildStatus = when {
                                            run.status != "completed" -> "ساخت در حال اجراست: " + run.status
                                            run.conclusion == "success" -> "ساخت APK موفق شد. برای دریافت فایل، گزارش اجرای GitHub را باز کن."
                                            else -> "ساخت ناموفق بود: " + (run.conclusion ?: "نامشخص") + "؛ گزارش را باز کن."
                                        }
                                    }
                                }.onFailure { buildStatus = it.message ?: "خواندن وضعیت ساخت ناموفق بود." }
                                buildBusy = false
                            }
                        }
                    }, enabled = !buildBusy && buildBranch.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        Text("بررسی وضعیت ساخت")
                    }
                    if (buildRunUrl.isNotBlank()) {
                        OutlinedButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(buildRunUrl)))
                        }, modifier = Modifier.fillMaxWidth()) { Text("بازکردن گزارش و دریافت APK در GitHub") }
                    }
                    Text("ساخت در GitHub Actions انجام می‌شود. خروجی APK به‌صورت Artifact در صفحه اجرای workflow قرار می‌گیرد؛ امضای نسخه انتشار نیازمند کلید امضای خودت است.", color = Muted, fontSize = 11.sp)
                    Divider(color = Muted.copy(alpha = 0.25f))
                    Text("ساخت سایت و وب‌اپ", color = Gold, fontWeight = FontWeight.Bold)
                    Text("پروژه فعال باید قالب وب HTML/CSS/JS یا React/Vite داشته باشد.", color = Muted, fontSize = 12.sp)
                    Text(webBuildStatus, color = Gold, fontSize = 12.sp)
                    Button(onClick = {
                        if (githubToken.isBlank()) {
                            webBuildStatus = "ابتدا GitHub را در همین صفحه متصل کن."
                        } else if (selectedProject.isBlank()) {
                            webBuildStatus = "ابتدا یک پروژه وب را باز کن."
                        } else {
                            webBuildBusy = true
                            webBuildStatus = "در حال اعتبارسنجی و ارسال فایل‌های سایت به GitHub..."
                            scope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        GitHubWebBuild.uploadProjectAndDispatch(
                                            githubToken, File(workspace, selectedProject), selectedProject
                                        )
                                    }
                                }.onSuccess { submission ->
                                    webBuildBranch = submission.branch
                                    webBuildProjectSlug = submission.projectSlug
                                    webBuildProjectName = selectedProject
                                    webBuildArtifactId = null
                                    cloudflarePreviewUrl = ""
                                    webBuildRunUrl = ""
                                    webBuildStatus = "پروژه وب ارسال شد و Build ابری آغاز شد؛ وضعیت را بررسی کن."
                                }.onFailure {
                                    webBuildStatus = "شروع Build وب ناموفق بود: " + (it.message ?: "خطای نامشخص")
                                }
                                webBuildBusy = false
                            }
                        }
                    }, enabled = !webBuildBusy && githubToken.isNotBlank() && selectedProject.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        if (webBuildBusy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Language, null)
                        Spacer(Modifier.width(8.dp)); Text("ارسال سایت و ساخت خروجی وب")
                    }
                    OutlinedButton(onClick = {
                        if (githubToken.isBlank() || webBuildBranch.isBlank()) {
                            webBuildStatus = "ابتدا پروژه وب را ارسال کن تا Build شروع شود."
                        } else {
                            webBuildBusy = true
                            scope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        GitHubWebBuild.latestBuild(githubToken, webBuildBranch, webBuildProjectSlug)
                                    }
                                }.onSuccess { run ->
                                    if (run == null) {
                                        webBuildStatus = "اجرای Build هنوز در GitHub ثبت نشده است؛ کمی بعد دوباره بررسی کن."
                                    } else {
                                        webBuildRunUrl = run.htmlUrl
                                        webBuildArtifactId = run.artifactId
                                        cloudflarePreviewUrl = run.previewUrl.orEmpty()
                                        webBuildStatus = when {
                                            run.status != "completed" -> "Build وب در حال اجراست: " + run.status
                                            run.buildSucceeded && run.previewUrl != null -> "ساخت موفق شد و پیش‌نمایش Cloudflare آماده است."
                                            run.buildSucceeded -> "ساخت موفق شد؛ خروجی dist آماده دریافت است."
                                            else -> "ساخت وب موفق نبود یا هنوز خروجی آماده نشده؛ گزارش را بررسی کن."
                                        }
                                        if (run.deploymentConclusion == "failure") {
                                            webBuildStatus += " انتشار Cloudflare ناموفق بود؛ جزئیات در گزارش GitHub است."
                                        }
                                    }
                                }.onFailure { webBuildStatus = it.message ?: "خواندن وضعیت Build وب ناموفق بود." }
                                webBuildBusy = false
                            }
                        }
                    }, enabled = !webBuildBusy && webBuildBranch.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        Text("بررسی وضعیت Build وب")
                    }
                    if (webBuildArtifactId != null && webBuildProjectName == selectedProject) {
                        OutlinedButton(onClick = {
                            webBuildBusy = true
                            webBuildStatus = "در حال دریافت امن خروجی سایت..."
                            scope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        GitHubWebBuild.downloadArtifact(
                                            githubToken, webBuildArtifactId!!, File(workspace, webBuildProjectName)
                                        )
                                    }
                                }.onSuccess {
                                    runCatching {
                                        selectedProject = webBuildProjectName
                                        refreshFiles()
                                        loadProjectPreview()
                                        page = "پیش‌نمایش"
                                    }.onSuccess {
                                        webBuildStatus = "خروجی دریافت شد؛ پیش‌نمایش محلی آماده است."
                                    }.onFailure {
                                        webBuildStatus = "خروجی دریافت شد اما پیش‌نمایش باز نشد: " + (it.message ?: "خطا")
                                    }
                                }.onFailure {
                                    webBuildStatus = "دریافت خروجی ناموفق بود: " + (it.message ?: "خطا")
                                }
                                webBuildBusy = false
                            }
                        }, enabled = !webBuildBusy, modifier = Modifier.fillMaxWidth()) {
                            Text("دریافت خروجی و پیش‌نمایش داخل برنامه")
                        }
                    }
                    if (cloudflarePreviewUrl.isNotBlank()) {
                        OutlinedButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cloudflarePreviewUrl)))
                        }, modifier = Modifier.fillMaxWidth()) { Text("بازکردن پیش‌نمایش آنلاین Cloudflare") }
                    }
                    if (webBuildRunUrl.isNotBlank()) {
                        OutlinedButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webBuildRunUrl)))
                        }, modifier = Modifier.fillMaxWidth()) { Text("بازکردن گزارش ساخت و انتشار") }
                    }
                    Text("ساخت وب در GitHub انجام می‌شود. پس از موفقیت می‌توانی خروجی واقعی را دریافت و در پیش‌نمایش داخلی باز کنی. برای انتشار آنلاین، تنظیم امن Cloudflare در Secrets و Variables مخزن لازم است.", color = Muted, fontSize = 11.sp)
                }
                "هوش مصنوعی" -> Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("استودیوی هوشمند ساخت برنامه", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(if (selectedProject.isBlank()) "ایده یا عکس را بفرست؛ برای تولید فایل‌ها پروژه‌ای را باز کن." else "پروژه فعال: $selectedProject", color = Muted, fontSize = 10.sp)
                        }
                        TextButton(onClick = { chatMessages.clear(); persistAiChat(); pendingAiPatches = emptyList(); pendingAiSummary = ""; reply = "" }) { Text("گفت‌وگوی جدید") }
                    }
                    if (selectedImageUri != null || visionStage.isNotBlank()) Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("مسیر بینایی تا ساخت برنامه", color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("۱. دریافت عکس  →  ۲. تحلیل بینایی  →  ۳. استخراج طراحی  →  ۴. تولید فایل‌های پروژه", color = Color.White, fontSize = 10.sp)
                            if (visionStage.isNotBlank()) Text(visionStage, color = if (visionStage.contains("خطا")) Color(0xFFFF8888) else Gold, fontSize = 11.sp)
                        }
                    }
                    if (chatMessages.isEmpty()) Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("از اینجا با هوش مصنوعی برنامه‌ات را طراحی کن", color = Gold, fontWeight = FontWeight.Bold)
                            Text("درخواستت را توضیح بده یا عکس نمونه برنامه را انتخاب کن. می‌توانی درباره امکانات صحبت کنی و بخواهی فایل‌های پروژه ساخته یا اصلاح شوند.", color = Color.White, fontSize = 12.sp)
                            Text("نمونه: «از روی این عکس یک صفحه ورود شبیه همین بساز و به پروژه فعلی اضافه کن.»", color = Muted, fontSize = 11.sp)
                        }
                    }
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(chatMessages) { message ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.role == "user") Arrangement.End else Arrangement.Start) {
                                Card(Modifier.fillMaxWidth(0.94f), colors = CardDefaults.cardColors(containerColor = if (message.role == "user") Color(0xFF183A63) else Panel)) {
                                    Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(if (message.role == "user") "شما" else "YASIN AI", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(message.content, color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        if (loading) item { Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp)); Text("در حال تحلیل درخواست و طراحی راه‌حل...", color = Muted, fontSize = 11.sp)
                        } }
                    }
                    if (selectedImageUri != null) Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, null, tint = Gold)
                        Text("تصویر انتخاب شد: $selectedImageName", color = Gold, fontSize = 10.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { selectedImageUri = null; selectedImageName = "" }) { Text("حذف عکس") }
                    }
                    OutlinedTextField(prompt, { prompt = it }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5,
                        label = { Text("چه برنامه‌ای بسازم یا چه چیزی را تغییر بدهم؟") })
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { aiImagePicker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.AddPhotoAlternate, null); Spacer(Modifier.width(4.dp)); Text("انتخاب عکس")
                        }
                        Spacer(Modifier.width(7.dp))
                        Button(onClick = {
                            val userPrompt = prompt.trim().ifBlank { "این تصویر را تحلیل کن و پیشنهاد بده چگونه آن را به برنامه واقعی تبدیل کنیم." }
                            val imageUri = selectedImageUri
                            val buildIntent = imageUri != null || listOf("بساز", "ایجاد کن", "طراحی کن", "تبدیل کن", "برنامه اندروید", "اپلیکیشن", "سایت بساز", "وب‌سایت").any { userPrompt.contains(it, ignoreCase = true) } ||
                                listOf("create", "build", "generate", "make an app", "make a website").any { userPrompt.contains(it, ignoreCase = true) }
                            val webIntent = listOf("وب‌سایت", "سایت بساز", "website", "web app").any { userPrompt.contains(it, ignoreCase = true) }
                            val activeRoot = if (selectedProject.isBlank()) null else File(workspace, selectedProject)
                            val hasAndroidBuild = activeRoot?.let { File(it, "settings.gradle.kts").isFile || File(it, "settings.gradle").isFile } == true
                            val hasWebEntry = activeRoot?.let { File(it, "index.html").isFile || File(it, "package.json").isFile } == true
                            val needsStarterProject = buildIntent && (selectedProject.isBlank() ||
                                (if (webIntent) !hasWebEntry else !hasAndroidBuild))
                            if (needsStarterProject) {
                                runCatching {
                                    val autoName = (if (webIntent) "AI-Web-" else "AI-Android-") + System.currentTimeMillis().toString()
                                    WorkspaceManager(workspace).createProject(autoName, if (webIntent) "web-static" else "android-kotlin")
                                    projects.add(0, autoName)
                                    persistProjects()
                                    selectedProject = autoName
                                    refreshFiles()
                                    notice = "پروژه پایه قابل ساخت ساخته شد: $autoName؛ فایل‌های هوش مصنوعی پس از بازبینی به همین پروژه اضافه می‌شوند."
                                }.onFailure { notice = "ساخت پروژه پایه ناموفق بود: " + (it.message ?: "خطا") }
                            }
                            chatMessages.add(StudioChatMessage("user", userPrompt + if (imageUri != null) "\n[تصویر پیوست شد]" else ""))
                            persistAiChat()
                            prompt = ""; selectedImageUri = null; selectedImageName = ""; loading = true
                            scope.launch {
                                val projectContext = if (selectedProject.isBlank()) "" else runCatching {
                                    val root = File(workspace, selectedProject)
                                    root.walkTopDown().filter { it.isFile && it.extension.lowercase() in setOf("kt", "java", "xml", "gradle", "kts", "properties", "json", "js", "jsx", "ts", "tsx", "html", "css", "md", "txt", "py") && !it.relativeTo(root).path.split(File.separator).any { part -> part.startsWith(".") } }
                                        .take(35).joinToString("\n") { file ->
                                            val rel = file.relativeTo(root).invariantSeparatorsPath
                                            "FILE: $rel\n" + if (file.length() <= 24000) file.readText().take(24000) else "[فایل بزرگ؛ محتوا ارسال نشد]"
                                        }.take(65000)
                                }.getOrDefault("")
                                val result = runCatching {
                                    val imageData = imageUri?.let { uri ->
                                        withContext(Dispatchers.IO) { prepareStudioImage(context, uri) }
                                    }
                                    if (imageData != null) {
                                        visionStage = "مرحله ۱ از ۲: در حال تحلیل تصویر و استخراج اجزای بصری..."
                                        val visualSpec = analyzeStudioImage(endpoint, model, apiKey, imageData)
                                        visionStage = "مرحله ۲ از ۲: تحلیل آماده شد؛ در حال تبدیل طراحی به فایل‌های واقعی پروژه..."
                                        askStudioModel(
                                            endpoint, model, apiKey,
                                            userPrompt + "\n\nمشخصات استخراج‌شده در مرحله بینایی (مبنای پیاده‌سازی دقیق):\n" + visualSpec +
                                                "\nبر اساس این مشخصات، رابط کاربری را در معماری و زبان پروژه فعلی پیاده‌سازی کن.",
                                            chatMessages.dropLast(1).takeLast(16), projectContext, null
                                        )
                                    } else {
                                        visionStage = ""
                                        askStudioModel(endpoint, model, apiKey, userPrompt, chatMessages.dropLast(1).takeLast(16), projectContext, null)
                                    }
                                }.getOrElse { StudioAiResult("خطا: " + (it.message ?: "نامشخص")) }
                                if (imageUri != null) visionStage = if (result.patches.isNotEmpty()) "تحلیل بینایی و تولید کد تمام شد؛ فایل‌ها برای بازبینی آماده‌اند." else "تحلیل تصویر انجام شد؛ برای تولید فایل، نتیجه و تنظیمات مدل را بررسی کن."
                                if (result.reply.startsWith("خطا:") || result.reply.startsWith("خطای سرویس")) visionStage = "خطا در فرایند بینایی/تولید: " + result.reply.take(180)
                                if (result.patches.isNotEmpty()) {
                                    pendingAiPatches = result.patches
                                    pendingAiSummary = result.summary
                                    notice = "تغییرات پیشنهادی آماده پیش‌نمایش است؛ هنوز هیچ فایلی تغییر نکرده."
                                }
                                chatMessages.add(StudioChatMessage("assistant", result.reply))
                                persistAiChat()
                                result.usage?.let { usage ->
                                    prefs.edit().putLong("usage_prompt", prefs.getLong("usage_prompt", 0L) + usage.promptTokens)
                                        .putLong("usage_completion", prefs.getLong("usage_completion", 0L) + usage.completionTokens)
                                        .putLong("usage_total", prefs.getLong("usage_total", 0L) + usage.totalTokens).apply()
                                    usageSummary = "توکن: ورودی " + prefs.getLong("usage_prompt", 0L) + " · خروجی " + prefs.getLong("usage_completion", 0L) + " · کل " + prefs.getLong("usage_total", 0L)
                                }
                                loading = false
                            }
                        }, enabled = !loading && (prompt.isNotBlank() || selectedImageUri != null) , modifier = Modifier.weight(1f)) {
                            if (loading) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Send, null)
                            Spacer(Modifier.width(5.dp)); Text("ارسال")
                        }
                    }
                    if (pendingAiPatches.isNotEmpty()) Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("تغییرات آماده بررسی: ${pendingAiPatches.size} فایل", color = Gold, fontWeight = FontWeight.Bold)
                            Text(pendingAiSummary, color = Color.White, fontSize = 11.sp)
                            Button(onClick = { showAiPatchPreview = true }, modifier = Modifier.fillMaxWidth()) { Text("پیش‌نمایش و تأیید فایل‌ها") }
                        }
                    }
                    if (usageSummary.isNotBlank()) Text(usageSummary, color = Muted, fontSize = 9.sp)
                    Text("هوش مصنوعی از طریق سرور امن Cloudflare متصل است؛ نیازی به کلید API شخصی نیست.", color = Gold, fontSize = 11.sp)
                }
                else -> Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("تنظیمات هوش مصنوعی", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("اتصال Cloudflare Workers AI فعال است", color = Gold, fontWeight = FontWeight.Bold)
                            Text("مدل: $model", color = Color.White, fontSize = 12.sp)
                            Text("نشانی سرویس: $endpoint", color = Muted, fontSize = 10.sp)
                            Text("برای استفاده از چت و تحلیل عکس، نیازی به واردکردن API URL یا کلید شخصی نیست.", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    Text("سهمیه رایگان مدل محدود است؛ در صورت اتمام سهمیه روزانه، سرویس تا زمان بازنشانی سهمیه پاسخ نمی‌دهد. مصرف بیشتر در طرح پولی ممکن است هزینه داشته باشد.", color = Gold, fontSize = 12.sp)
                }
            }
            NavigationBar(containerColor = Panel) {
                listOf("خانه", "پروژه‌ها", "GitHub", "هوش مصنوعی", "ویرایشگر", "پیش‌نمایش", "تنظیمات").forEach { label ->
                    NavigationBarItem(selected = page == label, onClick = { page = label },
                        icon = { Icon(when (label) {
                            "خانه" -> Icons.Default.Home
                            "پروژه‌ها" -> Icons.Default.Folder
                            "هوش مصنوعی" -> Icons.Default.AutoAwesome
                            "GitHub" -> Icons.Default.Link
                            "ویرایشگر" -> Icons.Default.Code
                            "پیش‌نمایش" -> Icons.Default.Visibility
                            else -> Icons.Default.Settings
                        }, contentDescription = label) }, label = { Text(label, fontSize = 9.sp) })
                }
            }
        }
    }

    if (showAiPatchPreview) AlertDialog(
        onDismissRequest = { showAiPatchPreview = false },
        title = { Text("پیش‌نمایش تغییرات هوش مصنوعی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 440.dp)) {
                Text(pendingAiSummary, color = Gold)
                Text("هیچ فایلی تا زمان تأیید تو تغییر نمی‌کند.", color = Muted, fontSize = 11.sp)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(pendingAiPatches) { patch ->
                        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                            Column(Modifier.padding(9.dp)) {
                                Text(patch.path, color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("${patch.content.lines().size} خط · ${patch.content.length} نویسه", color = Muted, fontSize = 10.sp)
                                Text(patch.content.take(280).replace("\n", " "), color = Color.White, fontSize = 10.sp, maxLines = 3)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = {
            runCatching {
                require(selectedProject.isNotBlank()) { "ابتدا یک پروژه را باز کن." }
                val manager = WorkspaceManager(workspace)
                pendingAiPatches.forEach { patch ->
                    manager.saveFile(selectedProject, patch.path, patch.content)
                }
                refreshFiles()
                notice = "${pendingAiPatches.size} فایل با تأیید تو در پروژه ذخیره شد؛ برای بررسی، فایل‌ها را باز کن."
                pendingAiPatches = emptyList(); pendingAiSummary = ""; showAiPatchPreview = false
            }.onFailure { notice = "اعمال تغییرات ناموفق بود: " + (it.message ?: "خطا") }
        }) { Text("تأیید و اعمال") } },
        dismissButton = { TextButton(onClick = { showAiPatchPreview = false }) { Text("فعلاً نه") } }
    )

    if (showHistory) AlertDialog(
        onDismissRequest = { showHistory = false },
        title = { Text("نسخه‌های قبلی فایل") },
        text = {
            if (historyItems.isEmpty()) {
                Text("برای این فایل هنوز نسخه قبلی ذخیره نشده است.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(historyItems, key = { it.file.name }) { snapshot ->
                        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(SimpleDateFormat("yyyy/MM/dd  HH:mm:ss", Locale.getDefault()).format(Date(snapshot.timestamp)), color = Color.White, fontSize = 12.sp)
                                    Text(snapshot.file.name, color = Muted, fontSize = 9.sp, maxLines = 1)
                                }
                                TextButton(onClick = {
                                    runCatching {
                                        val oldContent = snapshot.file.readText()
                                        WorkspaceManager(workspace).saveFile(selectedProject, selectedPath, oldContent)
                                        editorText = oldContent
                                        historyItems = FileHistory.list(workspace, selectedProject, selectedPath)
                                        notice = "نسخه انتخاب‌شده بازیابی شد؛ محتوای فعلی نیز در تاریخچه نگه‌داری شد."
                                        showHistory = false
                                    }.onFailure { notice = "بازیابی نسخه ناموفق بود: " + (it.message ?: "خطا") }
                                }) { Text("بازیابی") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { showHistory = false }) { Text("بستن") } }
    )

    if (showCreateProject) AlertDialog(onDismissRequest = { showCreateProject = false },
        title = { Text("ساخت پروژه") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(draftName, { draftName = it }, label = { Text("نام پروژه") }, singleLine = true)
            Text("قالب شروع", color = Gold, fontWeight = FontWeight.Bold)
            OutlinedTextField(templateSearch, { templateSearch = it }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, label = { Text("جست‌وجوی قالب") })
            ProjectTemplates.catalog.filter { template ->
                template.title.contains(templateSearch, true) || template.description.contains(templateSearch, true) ||
                    template.platform.contains(templateSearch, true)
            }.forEach { template ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selectedTemplate = template.id }) {
                    RadioButton(selected = selectedTemplate == template.id, onClick = { selectedTemplate = template.id })
                    Column {
                        Text(template.title, color = Color.White)
                        Text(template.description, color = Muted, fontSize = 11.sp)
                    }
                }
            }
        } },
        confirmButton = { TextButton(onClick = {
            val name = draftName.trim()
            if (name.isNotEmpty() && !projects.contains(name)) {
                runCatching {
                    val created = WorkspaceManager(workspace).createProject(name, selectedTemplate)
                    projects.add(0, name); persistProjects()
                    selectedProject = name
                    val readme = File(created, "README.md")
                    selectedPath = if (readme.isFile) "README.md" else ProjectTemplates.files(selectedTemplate, name).keys.first()
                    editorText = File(created, selectedPath).takeIf { it.isFile }?.readText().orEmpty()
                    refreshFiles(); draftName = ""; selectedTemplate = "blank"; templateSearch = ""; showCreateProject = false; page = "ویرایشگر"
                }.onFailure { notice = it.message ?: "ساخت پروژه ناموفق بود." }
            }
        }) { Text("ایجاد") } },
        dismissButton = { TextButton(onClick = { showCreateProject = false }) { Text("لغو") } })

    if (showSnippets) AlertDialog(
        onDismissRequest = { showSnippets = false },
        title = { Text("کتابخانه کد و اجزای آماده") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 480.dp)) {
                OutlinedTextField(snippetSearch, { snippetSearch = it }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, label = { Text("جست‌وجو: React، Kotlin، CSS و ...") })
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ComponentLibrary.search(snippetSearch)) { snippet ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                editorText = editorText.trimEnd() + if (editorText.isBlank()) snippet.code else "\n\n" + snippet.code
                                showSnippets = false
                                diagnosticMessage = "قطعه «${snippet.title}» به انتهای فایل اضافه شد؛ برای ذخیره، دکمه ذخیره را بزنید."
                            },
                            color = Panel, shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text(snippet.title, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("${snippet.language} · ${snippet.description}", color = Muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { showSnippets = false }) { Text("بستن") } }
    )

    if (showRenameProject) AlertDialog(
        onDismissRequest = { showRenameProject = false },
        title = { Text("تغییر نام پروژه") },
        text = { OutlinedTextField(projectNameDraft, { projectNameDraft = it }, label = { Text("نام جدید پروژه") }, singleLine = true) },
        confirmButton = { TextButton(onClick = {
            runCatching {
                WorkspaceManager(workspace).renameProject(projectToManage, projectNameDraft)
                val index = projects.indexOf(projectToManage)
                if (index >= 0) projects[index] = projectNameDraft.trim()
                if (selectedProject == projectToManage) selectedProject = projectNameDraft.trim()
                persistProjects(); refreshFiles(); notice = "نام پروژه تغییر کرد."
            }.onFailure { notice = it.message ?: "تغییر نام پروژه ناموفق بود." }
            showRenameProject = false
        }) { Text("ذخیره") } },
        dismissButton = { TextButton(onClick = { showRenameProject = false }) { Text("لغو") } }
    )

    if (showDeleteProject) AlertDialog(
        onDismissRequest = { showDeleteProject = false },
        title = { Text("حذف پروژه") },
        text = { Text("پروژه «$projectToManage» و تمام فایل‌های آن حذف شود؟ این کار قابل بازگشت نیست.") },
        confirmButton = { TextButton(onClick = {
            runCatching {
                WorkspaceManager(workspace).deleteProject(projectToManage)
                projects.remove(projectToManage)
                if (selectedProject == projectToManage) {
                    selectedProject = ""; fileItems = emptyList(); editorText = ""
                }
                persistProjects(); notice = "پروژه حذف شد."
            }.onFailure { notice = it.message ?: "حذف پروژه ناموفق بود." }
            showDeleteProject = false
        }) { Text("حذف") } },
        dismissButton = { TextButton(onClick = { showDeleteProject = false }) { Text("انصراف") } }
    )

    if (showCreateEntry) AlertDialog(onDismissRequest = { showCreateEntry = false },
        title = { Text("ساخت فایل یا پوشه") },
        text = { Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(createType == "file", { createType = "file" }); Text("فایل")
                Spacer(Modifier.width(12.dp))
                RadioButton(createType == "folder", { createType = "folder" }); Text("پوشه")
            }
            OutlinedTextField(draftName, { draftName = it }, label = { Text("نام یا مسیر نسبی، مانند src/Main.kt") })
        } },
        confirmButton = { TextButton(onClick = {
            if (draftName.isNotBlank()) {
                runCatching { WorkspaceManager(workspace).createEntry(selectedProject, draftName.trim(), createType == "folder") }
                    .onSuccess { refreshFiles(); draftName = ""; showCreateEntry = false }
                    .onFailure { notice = it.message ?: "ساخت فایل ناموفق بود." }
            }
        }) { Text("ایجاد") } },
        dismissButton = { TextButton(onClick = { showCreateEntry = false }) { Text("لغو") } })

    if (showRename) AlertDialog(onDismissRequest = { showRename = false },
        title = { Text("تغییر نام فایل یا پوشه") },
        text = { OutlinedTextField(renameText, { renameText = it }, label = { Text("نام جدید") }) },
        confirmButton = { TextButton(onClick = {
            val newName = renameText.trim()
            runCatching { WorkspaceManager(workspace).renameEntry(selectedProject, selectedPath, newName) }
                .onSuccess { refreshFiles() }
                .onFailure { notice = it.message ?: "تغییر نام ناموفق بود." }
            showRename = false
        }) { Text("تغییر نام") } },
        dismissButton = { TextButton(onClick = { showRename = false }) { Text("لغو") } })
}

private fun prepareStudioImage(context: Context, uri: Uri): String {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: error("خواندن تصویر ناموفق بود.")
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "فایل تصویر معتبر نیست." }

    var sample = 1
    while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > 1600) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.RGB_565 }
    val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: error("رمزگشایی تصویر ناموفق بود.")
    val output = java.io.ByteArrayOutputStream()
    try {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, output)
        val bytes = output.toByteArray()
        require(bytes.isNotEmpty() && bytes.size <= 5 * 1024 * 1024) {
            "آماده‌سازی تصویر ناموفق بود؛ لطفاً تصویر دیگری انتخاب کن."
        }
        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    } finally {
        bitmap.recycle()
        output.close()
    }
}

private suspend fun analyzeStudioImage(
    endpoint: String, model: String, key: String, imageDataUrl: String
): String = withContext(Dispatchers.IO) {
    require(endpoint.isNotBlank() && model.isNotBlank()) { "نشانی اتصال مدل بینایی تنظیم نشده است." }
    val safeEndpoint = AiEndpointPolicy.validate(endpoint)
    var conn: HttpURLConnection? = null
    try {
        conn = (URL(safeEndpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 120000
            doOutput = true
            instanceFollowRedirects = false
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (key.isNotBlank()) setRequestProperty("Authorization", "Bearer $key")
        }
        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content",
            "You are a senior computer-vision and UI reverse-engineering specialist. Inspect the supplied image carefully. Return STRICT JSON only: {\"analysis\":\"...\"}. Describe visible screen structure, hierarchy, components, spacing, alignment, colors with approximate hex values, typography, icons, controls, navigation, states, interactions inferable from the image, and responsive behavior. Separate observed facts from uncertain assumptions. Be detailed enough that a coding agent can recreate the interface. Do not return source code."))
        val content = JSONArray()
            .put(JSONObject().put("type", "text").put("text", "تحلیل بینایی دقیق این تصویر را انجام بده. تمام اجزای قابل مشاهده و ساختار صفحه را برای بازسازی دقیق استخراج کن."))
            .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", imageDataUrl)))
        messages.put(JSONObject().put("role", "user").put("content", content))
        val payload = JSONObject().put("model", model).put("messages", messages)
            .put("response_format", JSONObject().put("type", "json_object"))
        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val status = conn.responseCode
        val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        val raw = stream.bufferedReader().use { it.readText().take(2 * 1024 * 1024) }
        if (status !in 200..299) error("خطای سرویس بینایی ($status): " + raw.take(700))
        val response = JSONObject(raw)
        val message = response.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content")
        val cleaned = message.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val structuredAnalysis = runCatching { JSONObject(cleaned).optString("analysis") }.getOrNull()
        structuredAnalysis?.takeIf { it.isNotBlank() }
            ?: cleaned.takeIf { it.isNotBlank() }
            ?: error("مدل بینایی تحلیل قابل استفاده‌ای برنگرداند.")
    } finally {
        conn?.disconnect()
    }
}

private fun safeName(value: String): String = value.replace(Regex("[^A-Za-z0-9._ -]"), "_").trim().take(70).ifBlank { "project" }

private fun safeChild(root: File, relative: String): File? {
    val base = root.canonicalFile
    val target = File(base, relative).canonicalFile
    return if (target.path == base.path || target.path.startsWith(base.path + File.separator)) target else null
}

private data class AiCallResult(val text: String, val usage: AiUsage? = null)


private data class StudioAiResult(val reply: String, val summary: String = "", val patches: List<StudioFilePatch> = emptyList(), val usage: AiUsage? = null)

private suspend fun askStudioModel(
    endpoint: String, model: String, key: String, prompt: String,
    history: List<StudioChatMessage>, projectContext: String, imageDataUrl: String?
): StudioAiResult = withContext(Dispatchers.IO) {
    if (endpoint.isBlank() || model.isBlank()) return@withContext StudioAiResult("نشانی اتصال هوش مصنوعی تنظیم نشده است.")
    val safeEndpoint = runCatching { AiEndpointPolicy.validate(endpoint) }.getOrElse {
        return@withContext StudioAiResult("آدرس API نامعتبر است: " + (it.message ?: "فقط HTTPS مجاز است."))
    }
    var conn: HttpURLConnection? = null
    try {
        conn = (URL(safeEndpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 20000; readTimeout = 90000; doOutput = true
            instanceFollowRedirects = false
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (key.isNotBlank()) setRequestProperty("Authorization", "Bearer $key")
        }
        val messages = JSONArray()
        val systemPrompt = """You are YASIN AI, a senior Persian-speaking software engineer, product designer and coding agent.
Talk naturally in Persian. Understand app requirements, ask useful questions, explain tradeoffs, and inspect supplied screenshots.
When the user asks to create or modify project files, return STRICT JSON only with this schema:
{"reply":"Persian user-facing response","summary":"short change summary","files":[{"path":"relative/path","content":"complete file content"}]}
For ordinary conversation/questions, return {"reply":"...","summary":"","files":[]}.
For straightforward build requests (calculator, notes, todo list, timer, login screen, simple game or landing page), choose sensible defaults and generate a complete working first version immediately; do not ask follow-up questions unless a critical requirement is missing.
Never claim code was written unless you provide it in files. Provide complete file contents, not diffs. Use only paths relative to the selected project root. Never emit absolute paths, .. segments, hidden paths, secrets, or executable shell scripts. Do not overwrite build outputs, .git, .history, gradle caches, local.properties, keystores, signing configs or credential files. Keep existing Gradle project configuration intact and only create or modify app source, resources, tests and documentation as needed. If context is insufficient, make reasonable defaults for a simple starter instead of returning no files.
For image requests, infer layout, colors, typography, controls and navigation from the image; create actual app UI source files consistent with the existing project's language and architecture. Preserve existing project structure and avoid unnecessary rewrites.
For the supplied Android Kotlin + Jetpack Compose template, preserve the existing package declaration and edit the existing app/src/main/java/.../MainActivity.kt path shown in the project context. Use androidx.compose.material3 APIs (not androidx.compose.material), match the dependencies already present in app/build.gradle.kts, and return complete compilable Kotlin files. Do not invent XML layouts for a Compose project.
Current selected project files:
$projectContext"""
        messages.put(JSONObject().put("role", "system").put("content", systemPrompt))
        history.takeLast(16).forEach { item ->
            if (item.role == "user" || item.role == "assistant") messages.put(JSONObject().put("role", item.role).put("content", item.content.take(10000)))
        }
        val userContent: Any = if (imageDataUrl.isNullOrBlank()) prompt else JSONArray()
            .put(JSONObject().put("type", "text").put("text", prompt.ifBlank { "این تصویر را به رابط کاربری واقعی در پروژه تبدیل کن." }))
            .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", imageDataUrl)))
        messages.put(JSONObject().put("role", "user").put("content", userContent))
        val fileSchema = JSONObject()
            .put("type", "object")
            .put("properties", JSONObject()
                .put("path", JSONObject().put("type", "string"))
                .put("content", JSONObject().put("type", "string")))
            .put("required", JSONArray().put("path").put("content"))
            .put("additionalProperties", false)
        val outputSchema = JSONObject()
            .put("type", "object")
            .put("properties", JSONObject()
                .put("reply", JSONObject().put("type", "string"))
                .put("summary", JSONObject().put("type", "string"))
                .put("files", JSONObject().put("type", "array").put("items", fileSchema)))
            .put("required", JSONArray().put("reply").put("summary").put("files"))
            .put("additionalProperties", false)
        val responseFormat = JSONObject()
            .put("type", "json_schema")
            .put("json_schema", outputSchema)
        val payload = JSONObject().put("model", model).put("messages", messages)
            .put("response_format", responseFormat)
        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val status = conn.responseCode
        val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        val raw = stream.bufferedReader().use { it.readText().take(2 * 1024 * 1024) }
        if (status !in 200..299) return@withContext StudioAiResult("خطای سرویس ($status): " + raw.take(1000))
        val response = JSONObject(raw)
        val content = response.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content")
        val jsonText = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val parsed = runCatching { JSONObject(jsonText) }.getOrNull()
            ?: return@withContext StudioAiResult(jsonText.take(12000), usage = AiUsageParser.parse(raw))
        val patches = mutableListOf<StudioFilePatch>()
        val files = parsed.optJSONArray("files") ?: JSONArray()
        require(files.length() <= 40) { "تعداد فایل‌های پیشنهادی بیش از حد مجاز است." }
        for (i in 0 until files.length()) {
            val item = files.getJSONObject(i)
            val path = item.optString("path")
            val body = item.optString("content")
            val parts = path.split('/')
            require(path.isNotBlank() && !path.startsWith('/') && !path.contains('\\') &&
                parts.none { it.isBlank() || it == "." || it == ".." || it.startsWith(".") }) { "مسیر فایل ناامن است: $path" }
            require(!path.startsWith("build/") && !path.startsWith(".git/") &&
                path !in listOf("local.properties", "gradle.properties", "settings.gradle", "settings.gradle.kts") &&
                !path.contains("keystore", true) && !path.contains("secret", true) && !path.contains("credential", true)) {
                "تغییر فایل حساس مسدود شد: $path"
            }
            require(body.toByteArray(Charsets.UTF_8).size <= 1024 * 1024) { "حجم فایل پیشنهادی بیش از حد مجاز است." }
            patches += StudioFilePatch(path, body)
        }
        StudioAiResult(parsed.optString("reply", "پاسخ آماده شد."), parsed.optString("summary"), patches, AiUsageParser.parse(raw))
    } catch (e: Exception) {
        StudioAiResult("نتوانستم پاسخ ساختاریافته را پردازش کنم: " + (e.message ?: "خطای نامشخص"))
    } finally { conn?.disconnect() }
}
