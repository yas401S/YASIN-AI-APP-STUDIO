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
import java.net.HttpURLConnection
import java.net.URL

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

@Composable
private fun StudioApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("studio", 0) }
    var page by remember { mutableStateOf("خانه") }
    val projects = remember {
        mutableStateListOf<String>().apply {
            try {
                val saved = JSONArray(prefs.getString("projects", "[]"))
                for (i in 0 until saved.length()) add(saved.getString(i))
            } catch (_: Exception) { }
        }
    }
    var selected by remember { mutableStateOf("") }
    var draftName by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("// کد پروژه خود را اینجا بنویسید") }
    var prompt by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf(prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions") ?: "") }
    var model by remember { mutableStateOf(prefs.getString("model", "gpt-4o-mini") ?: "") }
    var apiKey by remember { mutableStateOf(prefs.getString("key", "") ?: "") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun saveProjects() {
        prefs.edit().putString("projects", JSONArray(projects).toString()).apply()
    }

    Surface(Modifier.fillMaxSize(), color = Navy) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().background(Panel).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = Gold, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("YASIN AI APP STUDIO", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text("محیط ساخت نرم‌افزار با هوش مصنوعی", color = Muted, fontSize = 11.sp)
                }
            }
            when (page) {
                "خانه" -> Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("از ایده تا نرم‌افزار", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("پروژه بساز، کد بنویس و از دستیار هوشمند کمک بگیر.", color = Muted)
                    Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp)) {
                            Text("پروژه‌های شما", color = Gold)
                            Text(projects.size.toString(), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("ساخت پروژه جدید")
                            }
                        }
                    }
                    listOf("پروژه‌ها" to "مدیریت فایل‌ها و پروژه‌ها", "هوش مصنوعی" to "گفت‌وگو و کمک در برنامه‌نویسی", "ویرایشگر" to "نوشتن و ذخیره کد", "تنظیمات" to "اتصال سرویس هوش مصنوعی").forEach { item ->
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
                "پروژه‌ها" -> Column(Modifier.fillMaxSize().padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("پروژه‌های من", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Button(onClick = { showCreate = true }) { Text("پروژه جدید") }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (projects.isEmpty()) Text("هنوز پروژه‌ای ساخته نشده است.", color = Muted)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(projects) { name ->
                            Card(Modifier.fillMaxWidth().clickable {
                                selected = name
                                code = prefs.getString("code_" + name, "// " + name) ?: ""
                                page = "ویرایشگر"
                            }, colors = CardDefaults.cardColors(containerColor = Panel)) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Folder, null, tint = Gold)
                                    Spacer(Modifier.width(12.dp))
                                    Text(name, color = Color.White, modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ChevronRight, null, tint = Muted)
                                }
                            }
                        }
                    }
                }
                "ویرایشگر" -> Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("ویرایشگر کد", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(if (selected.isBlank()) "فایل موقت" else selected, color = Muted, fontSize = 12.sp)
                        }
                        Button(onClick = {
                            if (selected.isNotBlank()) prefs.edit().putString("code_" + selected, code).apply()
                        }, enabled = selected.isNotBlank()) { Icon(Icons.Default.Save, null); Text("ذخیره") }
                    }
                    BasicTextField(
                        value = code, onValueChange = { code = it },
                        modifier = Modifier.fillMaxSize().background(Color(0xFF050D18), RoundedCornerShape(10.dp)).padding(14.dp),
                        textStyle = TextStyle(color = Color(0xFFB8D8FF), fontSize = 13.sp, lineHeight = 20.sp)
                    )
                }
                "هوش مصنوعی" -> Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("دستیار هوشمند", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("درخواست برنامه‌نویسی خود را بنویسید.", color = Muted)
                    OutlinedTextField(prompt, { prompt = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, label = { Text("درخواست شما") })
                    Button(onClick = {
                        loading = true
                        scope.launch {
                            reply = askModel(endpoint, model, apiKey, prompt)
                            loading = false
                        }
                    }, enabled = !loading && prompt.isNotBlank() && apiKey.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Send, null)
                        Spacer(Modifier.width(8.dp)); Text(if (loading) "در حال پاسخ..." else "ارسال درخواست")
                    }
                    if (reply.isNotBlank()) Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                        Text(reply, Modifier.padding(14.dp), color = Color.White)
                    }
                    if (apiKey.isBlank()) Text("برای فعال‌سازی، ابتدا اطلاعات API را در تنظیمات وارد کنید.", color = Gold, fontSize = 12.sp)
                }
                else -> Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("تنظیمات هوش مصنوعی", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("سرویس سازگار با OpenAI Chat Completions", color = Muted)
                    OutlinedTextField(endpoint, { endpoint = it }, modifier = Modifier.fillMaxWidth(), label = { Text("آدرس API") }, singleLine = true)
                    OutlinedTextField(model, { model = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نام مدل") }, singleLine = true)
                    OutlinedTextField(apiKey, { apiKey = it }, modifier = Modifier.fillMaxWidth(), label = { Text("کلید API") }, singleLine = true)
                    Button(onClick = {
                        prefs.edit().putString("endpoint", endpoint).putString("model", model).putString("key", apiKey).apply()
                    }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Save, null); Text("ذخیره تنظیمات") }
                    Text("این نسخه اولیه کلید را در حافظه خصوصی برنامه نگه می‌دارد. پیش از انتشار عمومی باید به سرور امن منتقل شود.", color = Gold, fontSize = 12.sp)
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
                        }, contentDescription = label) },
                        label = { Text(label, fontSize = 9.sp) })
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(onDismissRequest = { showCreate = false },
            title = { Text("ساخت پروژه") },
            text = { OutlinedTextField(draftName, { draftName = it }, label = { Text("نام پروژه") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    val name = draftName.trim()
                    if (name.isNotEmpty() && !projects.contains(name)) {
                        projects.add(0, name)
                        saveProjects()
                        selected = name
                        code = "// " + name + "\n"
                        prefs.edit().putString("code_" + name, code).apply()
                        draftName = ""
                        showCreate = false
                        page = "ویرایشگر"
                    }
                }) { Text("ایجاد") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("لغو") } }
        )
    }
}

private suspend fun askModel(endpoint: String, model: String, key: String, prompt: String): String = withContext(Dispatchers.IO) {
    if (endpoint.isBlank() || model.isBlank() || key.isBlank()) return@withContext "ابتدا تنظیمات اتصال را کامل کنید."
    var conn: HttpURLConnection? = null
    try {
        conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 20000; readTimeout = 60000; doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer " + key)
        }
        val payload = JSONObject().put("model", model).put("messages",
            JSONArray().put(JSONObject().put("role", "system").put("content", "You are a careful software engineering assistant."))
                .put(JSONObject().put("role", "user").put("content", prompt)))
        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val status = conn.responseCode
        val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        val raw = stream.bufferedReader().use { it.readText() }
        if (status !in 200..299) "خطای سرویس (" + status + "): " + raw.take(1200)
        else JSONObject(raw).getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content", "پاسخ خالی بود.")
    } catch (e: Exception) {
        "خطا در ارتباط: " + (e.message ?: "نامشخص")
    } finally { conn?.disconnect() }
}
