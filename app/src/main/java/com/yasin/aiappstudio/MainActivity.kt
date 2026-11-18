package com.yasin.aiappstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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
    var fileItems by remember { mutableStateOf(listOf<WorkspaceFile>()) }
    var draftName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf("blank") }
    var createType by remember { mutableStateOf("file") }
    var showCreateProject by remember { mutableStateOf(false) }
    var showCreateEntry by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf(prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions") ?: "") }
    var model by remember { mutableStateOf(prefs.getString("model", "gpt-4o-mini") ?: "") }
    var apiKey by remember { mutableStateOf(prefs.getString("key", "") ?: "") }
    var loading by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun projectDir(): File = File(workspace, selectedProject)
    fun refreshFiles() {
        val root = projectDir()
        fileItems = if (root.exists()) root.walkTopDown()
            .filter { it != root && !it.relativeTo(root).path.split(File.separator).any { p -> p.startsWith(".") } }
            .map { WorkspaceFile(it, it.relativeTo(root).invariantSeparatorsPath, it.isDirectory) }
            .sortedWith(compareBy<WorkspaceFile>({ !it.directory }, { it.relative.lowercase() })).toList()
        else emptyList()
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
                        Button(onClick = { showCreateProject = true }) { Icon(Icons.Default.Add, null); Text("پروژه") }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (selectedProject.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("پروژه فعال: $selectedProject", color = Gold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { showCreateEntry = true }) { Text("فایل/پوشه جدید") }
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
                "ویرایشگر" -> Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("ویرایشگر کد", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Text("$selectedProject / $selectedPath", color = Muted, fontSize = 11.sp)
                        }
                        Button(onClick = { saveFile() }, enabled = selectedProject.isNotBlank()) { Icon(Icons.Default.Save, null); Text("ذخیره") }
                    }
                    BasicTextField(value = editorText, onValueChange = { editorText = it },
                        modifier = Modifier.fillMaxSize().background(Color(0xFF050D18), RoundedCornerShape(10.dp)).padding(14.dp),
                        textStyle = TextStyle(color = Color(0xFFB8D8FF), fontSize = 13.sp, lineHeight = 20.sp))
                    if (notice.isNotBlank()) Text(notice, color = Gold, fontSize = 11.sp)
                }
                "هوش مصنوعی" -> Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("دستیار هوشمند", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("درخواست برنامه‌نویسی خود را وارد کنید.", color = Muted)
                    OutlinedTextField(prompt, { prompt = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, label = { Text("درخواست شما") })
                    Button(onClick = {
                        loading = true
                        scope.launch { reply = askModel(endpoint, model, apiKey, prompt); loading = false }
                    }, enabled = !loading && prompt.isNotBlank() && apiKey.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Send, null)
                        Spacer(Modifier.width(8.dp)); Text(if (loading) "در حال پاسخ..." else "ارسال درخواست")
                    }
                    if (reply.isNotBlank()) Card(colors = CardDefaults.cardColors(containerColor = Panel)) { Text(reply, Modifier.padding(14.dp), color = Color.White) }
                    if (apiKey.isBlank()) Text("برای فعال‌سازی، تنظیمات API را کامل کنید.", color = Gold, fontSize = 12.sp)
                }
                else -> Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("تنظیمات هوش مصنوعی", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("سرویس سازگار با OpenAI Chat Completions", color = Muted)
                    OutlinedTextField(endpoint, { endpoint = it }, modifier = Modifier.fillMaxWidth(), label = { Text("آدرس API") }, singleLine = true)
                    OutlinedTextField(model, { model = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نام مدل") }, singleLine = true)
                    OutlinedTextField(apiKey, { apiKey = it }, modifier = Modifier.fillMaxWidth(), label = { Text("کلید API") }, singleLine = true)
                    Button(onClick = { prefs.edit().putString("endpoint", endpoint).putString("model", model).putString("key", apiKey).apply(); notice = "تنظیمات ذخیره شد." }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Save, null); Text("ذخیره تنظیمات")
                    }
                    Text("هشدار: نگهداری مستقیم کلید در برنامه فقط برای آزمایش شخصی است. نسخه عمومی باید از درگاه امن سمت سرور استفاده کند.", color = Gold, fontSize = 12.sp)
                }
            }
            NavigationBar(containerColor = Panel) {
                listOf("خانه", "پروژه‌ها", "هوش مصنوعی", "ویرایشگر", "تنظیمات").forEach { label ->
                    NavigationBarItem(selected = page == label, onClick = { page = label },
                        icon = { Icon(when (label) {
                            "خانه" -> Icons.Default.Home
                            "پروژه‌ها" -> Icons.Default.Folder
                            "هوش مصنوعی" -> Icons.Default.AutoAwesome
                            "ویرایشگر" -> Icons.Default.Code
                            else -> Icons.Default.Settings
                        }, contentDescription = label) }, label = { Text(label, fontSize = 9.sp) })
                }
            }
        }
    }

    if (showCreateProject) AlertDialog(onDismissRequest = { showCreateProject = false },
        title = { Text("ساخت پروژه") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(draftName, { draftName = it }, label = { Text("نام پروژه") }, singleLine = true)
            Text("قالب شروع", color = Gold, fontWeight = FontWeight.Bold)
            ProjectTemplates.catalog.forEach { template ->
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
                    refreshFiles(); draftName = ""; selectedTemplate = "blank"; showCreateProject = false; page = "ویرایشگر"
                }.onFailure { notice = it.message ?: "ساخت پروژه ناموفق بود." }
            }
        }) { Text("ایجاد") } },
        dismissButton = { TextButton(onClick = { showCreateProject = false }) { Text("لغو") } })

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

private fun safeName(value: String): String = value.replace(Regex("[^A-Za-z0-9._ -]"), "_").trim().take(70).ifBlank { "project" }

private fun safeChild(root: File, relative: String): File? {
    val base = root.canonicalFile
    val target = File(base, relative).canonicalFile
    return if (target.path == base.path || target.path.startsWith(base.path + File.separator)) target else null
}

private suspend fun askModel(endpoint: String, model: String, key: String, prompt: String): String = withContext(Dispatchers.IO) {
    if (endpoint.isBlank() || model.isBlank() || key.isBlank()) return@withContext "ابتدا تنظیمات اتصال را کامل کنید."
    var conn: HttpURLConnection? = null
    try {
        conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 20000; readTimeout = 60000; doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $key")
        }
        val payload = JSONObject().put("model", model).put("messages",
            JSONArray().put(JSONObject().put("role", "system").put("content", "You are a careful software engineering assistant."))
                .put(JSONObject().put("role", "user").put("content", prompt)))
        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val status = conn.responseCode
        val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        val raw = stream.bufferedReader().use { it.readText() }
        if (status !in 200..299) "خطای سرویس ($status): " + raw.take(1200)
        else JSONObject(raw).getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content", "پاسخ خالی بود.")
    } catch (e: Exception) {
        "خطا در ارتباط: " + (e.message ?: "نامشخص")
    } finally { conn?.disconnect() }
}
