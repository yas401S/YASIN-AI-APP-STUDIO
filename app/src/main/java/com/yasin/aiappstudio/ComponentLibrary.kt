package com.yasin.aiappstudio

/** Offline editable snippets. Insertion never executes code or accesses the network. */
object ComponentLibrary {
    data class Snippet(val id: String, val title: String, val language: String, val description: String, val code: String)

    val snippets = listOf(
        Snippet("html-section", "بخش معنایی HTML", "HTML", "بخش واکنش‌پذیر با عنوان و توضیح", """<section class="section" aria-labelledby="section-title">
  <h2 id="section-title">عنوان بخش</h2>
  <p>توضیح بخش را اینجا بنویسید.</p>
</section>"""),
        Snippet("html-form", "فرم قابل‌دسترس", "HTML", "ورودی دارای label و اعتبارسنجی مرورگر", """<form>
  <label for="email">ایمیل</label>
  <input id="email" name="email" type="email" autocomplete="email" required>
  <button type="submit">ارسال</button>
</form>"""),
        Snippet("css-card", "کارت واکنش‌گرا", "CSS", "کارت با فاصله و گوشه استاندارد", """.card {
  padding: 1.25rem;
  border: 1px solid #dbe3ef;
  border-radius: 1rem;
  background: white;
  box-shadow: 0 12px 32px #10204012;
}"""),
        Snippet("css-grid", "شبکه واکنش‌گرا", "CSS", "چیدمان خودکار با حداقل عرض", """.responsive-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 240px), 1fr));
  gap: 1rem;
}"""),
        Snippet("js-fetch", "درخواست امن API", "JavaScript", "مدیریت خطا و پاسخ HTTP", """async function loadData(url) {
  const response = await fetch(url, { headers: { Accept: "application/json" } });
  if (!response.ok) throw new Error("HTTP " + response.status);
  return response.json();
}"""),
        Snippet("js-debounce", "Debounce جست‌وجو", "JavaScript", "کاهش درخواست‌های تکراری", """function debounce(callback, delay = 300) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => callback(...args), delay);
  };
}"""),
        Snippet("react-component", "کامپوننت React", "React", "کامپوننت تابعی ساده", """export function InfoCard({ title, children }) {
  return (
    <section className="info-card" aria-label={title}>
      <h2>{title}</h2>
      <div>{children}</div>
    </section>
  );
}"""),
        Snippet("kotlin-compose", "کامپوننت Compose", "Kotlin", "کارت ساده Jetpack Compose", """@Composable
fun InfoCard(title: String, description: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}"""),
        Snippet("kotlin-state", "وضعیت Compose", "Kotlin", "حالت قابل مشاهده و دکمه", """@Composable
fun Counter() {
    var count by remember { mutableIntStateOf(0) }
    Button(onClick = { count++ }) {
        Text("تعداد: " + count)
    }
}"""),
        Snippet("godot-player", "حرکت بازیکن Godot", "GDScript", "حرکت دوبعدی با ورودی جهت", """extends CharacterBody2D

@export var speed := 240.0

func _physics_process(_delta: float) -> void:
    var direction := Input.get_vector("ui_left", "ui_right", "ui_up", "ui_down")
    velocity = direction * speed
    move_and_slide()""")
    )

    fun search(query: String, language: String = "همه"): List<Snippet> {
        val q = query.trim()
        return snippets.filter { snippet ->
            (language == "همه" || snippet.language == language) &&
                (q.isBlank() || snippet.title.contains(q, true) ||
                    snippet.description.contains(q, true) || snippet.language.contains(q, true))
        }
    }
}
