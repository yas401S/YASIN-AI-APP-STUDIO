package com.yasin.aiappstudio

/** Offline starter catalog. Templates generate complete, editable source projects. */
object ProjectTemplates {
    data class Template(val id: String, val title: String, val platform: String, val description: String)

    val catalog = listOf(
        Template("blank", "پروژه خالی", "عمومی", "ساختار ساده برای شروع آزاد"),
        Template("web-static", "وب‌سایت کامل HTML/CSS/JS", "وب", "ریسپانسیو، راست‌چین، سئو، دسترس‌پذیری و تعامل واقعی"),
        Template("web-react", "وب‌اپ React + Vite", "وب", "ساختار ماژولار، نسخه‌های ثابت و اسکریپت build"),
        Template("android-kotlin", "اپلیکیشن Android Kotlin + Compose", "اندروید", "پروژه Gradle کامل با Compose، تم، منابع و تست"),
        Template("godot", "بازی Godot 2D", "بازی", "پروژه پایه Godot با صحنه اصلی")
    )

    fun files(templateId: String, projectName: String): Map<String, String> {
        val dollar = 36.toChar()
        val safeTitle = projectName.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$")
        val slug = projectName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(48).ifBlank { "new-project" }
        val appId = "com.yasin.generated.app_" + slug.replace('-', '_')
        val packagePath = appId.replace('.', '/')
        return when (templateId) {
            "blank" -> mapOf("README.md" to "# $projectName\n\nساخته‌شده با YASIN AI APP STUDIO.\n")
            "web-static" -> mapOf(
                "README.md" to """# $projectName

وب‌سایت ایستای آماده توسعه با HTML5، CSS و JavaScript خالص؛ بدون وابستگی یا نصب پکیج.

## اجرا
- فایل index.html را در پیش‌نمایش وب استودیو باز کنید.
- برای توسعه محلی، پوشه را با یک static server مانند python -m http.server 8000 سرو کنید.
- فرم تماس فقط اعتبارسنجی مرورگر دارد و پیام را ارسال نمی‌کند.

## انتشار روی Cloudflare Pages
1. پوشه را به مخزن GitHub خود push کنید.
2. در Cloudflare Dashboard بخش Workers & Pages، یک Pages project متصل به Git بسازید.
3. Build command را خالی و Build output directory را نقطه (.) قرار دهید.
4. دامنه واقعی را در sitemap.xml و robots.txt جایگزین example.com کنید.

## ساختار
- index.html: صفحه اصلی معنایی و RTL
- assets/css/styles.css: طراحی واکنش‌گرا و حالت تیره
- assets/js/app.js: منوی موبایل، تم، FAQ و اعتبارسنجی فرم
- 404.html، robots.txt و sitemap.xml
""",
                "index.html" to """<!doctype html>
<html lang="fa" dir="rtl">
<head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="theme-color" content="#101827"><meta name="description" content="قالب وب‌سایت فارسی، راست‌چین و واکنش‌گرا.">
<meta property="og:type" content="website"><meta property="og:title" content="$safeTitle">
<meta property="og:description" content="وب‌سایت حرفه‌ای و واکنش‌گرا"><meta property="og:image" content="assets/og-cover.svg">
<title>$safeTitle</title><link rel="icon" href="assets/favicon.svg" type="image/svg+xml">
<link rel="manifest" href="site.webmanifest"><link rel="stylesheet" href="assets/css/styles.css">
</head>
<body>
<a class="skip-link" href="#main">رفتن به محتوای اصلی</a>
<header class="site-header"><div class="container nav-wrap">
<a class="brand" href="#home"><span class="brand-mark" aria-hidden="true">Y</span><span>$safeTitle</span></a>
<button class="icon-button menu-toggle" type="button" aria-expanded="false" aria-controls="primary-nav" aria-label="باز کردن منو">☰</button>
<nav id="primary-nav" class="primary-nav" aria-label="منوی اصلی">
<a href="#features">امکانات</a><a href="#about">درباره</a><a href="#faq">پرسش‌ها</a><a class="nav-cta" href="#contact">تماس</a>
<button class="icon-button theme-toggle" type="button" aria-label="تغییر حالت رنگ">◐</button></nav>
</div></header>
<main id="main">
<section class="hero container" id="home"><div class="hero-copy">
<span class="eyebrow">قالب آماده · قابل ویرایش · فارسی</span>
<h1>ایده‌ات را به یک <span>وب‌سایت واقعی</span> تبدیل کن.</h1>
<p class="hero-lead">نقطه شروعی سبک با ساختار استاندارد، طراحی واکنش‌گرا و کدهایی که به‌سادگی می‌توانی تغییر بدهی.</p>
<div class="hero-actions"><a class="button button-primary" href="#features">مشاهده امکانات</a><a class="button button-secondary" href="#about">درباره پروژه</a></div>
<ul class="trust-list"><li>بدون کتابخانه خارجی</li><li>سازگار با موبایل</li><li>راست‌چین و دسترس‌پذیر</li></ul>
</div><div class="hero-art" role="img" aria-label="نمای گرافیکی وب‌سایت"><div class="art-window">
<div class="window-dots"><i></i><i></i><i></i></div><div class="art-line wide"></div><div class="art-line"></div>
<div class="art-grid"><span></span><span></span><span></span></div><div class="art-line short"></div>
</div><span class="floating-chip chip-top">HTML · CSS</span><span class="floating-chip chip-bottom">JavaScript</span></div>
</section>
<section class="section section-soft" id="features"><div class="container">
<div class="section-heading"><span class="eyebrow">امکانات</span><h2>همه‌چیز برای شروع اصولی</h2><p>هر بخش نمونه است و می‌توانی آن را متناسب با نیازت توسعه بدهی.</p></div>
<div class="feature-grid">
<article class="feature-card"><span class="feature-icon">▦</span><h3>واکنش‌گرا</h3><p>چیدمان سازگار با موبایل، تبلت و دسکتاپ.</p></article>
<article class="feature-card"><span class="feature-icon">⌘</span><h3>کد تمیز</h3><p>HTML معنایی، CSS مرتب و JavaScript بدون وابستگی.</p></article>
<article class="feature-card"><span class="feature-icon">◐</span><h3>روشن و تیره</h3><p>انتخاب پوسته و ذخیره تنظیم در مرورگر.</p></article>
<article class="feature-card"><span class="feature-icon">◎</span><h3>آماده توسعه</h3><p>فایل‌های جدا، متادیتا و صفحه 404.</p></article>
</div></div></section>
<section class="section container" id="about"><div class="about-panel"><div><span class="eyebrow">درباره پروژه</span><h2>ساده شروع کن؛ حرفه‌ای گسترش بده.</h2></div>
<p>متن‌ها، رنگ‌ها و بخش‌ها نمونه هستند. آن‌ها را با محتوای واقعی محصولت جایگزین کن. این قالب ادعای ارسال پیام یا اتصال به سرویس خارجی ندارد.</p></div></section>
<section class="section section-soft" id="faq"><div class="container faq-wrap"><div class="section-heading"><span class="eyebrow">راهنما</span><h2>پرسش‌های متداول</h2></div>
<details><summary>برای اجرا به نصب کتابخانه نیاز دارم؟</summary><p>خیر؛ نسخه HTML/CSS/JavaScript با مرورگر و یک static server اجرا می‌شود.</p></details>
<details><summary>چطور نام و رنگ سایت را عوض کنم؟</summary><p>عنوان را در index.html و رنگ‌ها را در متغیرهای ابتدای styles.css تغییر بده.</p></details>
<details><summary>آیا فرم تماس پیام را ارسال می‌کند؟</summary><p>خیر؛ فرم نمونه فقط ورودی‌ها را بررسی می‌کند. برای ارسال واقعی باید backend یا سرویس فرم امن متصل کنی.</p></details>
</div></section>
<section class="section container" id="contact"><div class="contact-panel"><div><span class="eyebrow">شروع گفتگو</span><h2>برای پروژه‌ات برنامه داری؟</h2><p>این فرم نمونه است و اطلاعات را به سرور نمی‌فرستد؛ بعداً آن را به API امن خودت متصل کن.</p></div>
<form id="contact-form" novalidate><label for="contact-name">نام</label><input id="contact-name" name="name" autocomplete="name" required minlength="2">
<label for="contact-email">ایمیل</label><input id="contact-email" name="email" type="email" autocomplete="email" required>
<label for="contact-message">پیام</label><textarea id="contact-message" name="message" rows="3" required minlength="5"></textarea>
<button class="button button-primary" type="submit">بررسی فرم نمونه</button><p id="form-status" class="form-status" role="status" aria-live="polite"></p></form>
</div></section>
</main>
<footer class="site-footer"><div class="container footer-inner"><span>© <span id="current-year"></span> $safeTitle</span><a href="#home">بازگشت به بالا ↑</a></div></footer>
<script src="assets/js/app.js"></script>
</body></html>
""",
                "assets/css/styles.css" to """:root{color-scheme:light;--bg:#fff;--surface:#f3f6fb;--text:#17243a;--muted:#61718a;--primary:#315ee8;--line:#dfe6f0;--card:#fff;--shadow:0 18px 55px #17243a12}
:root[data-theme="dark"]{color-scheme:dark;--bg:#101827;--surface:#172338;--text:#edf3ff;--muted:#a8b6cc;--primary:#83a5ff;--line:#2b3b55;--card:#1b2a40;--shadow:0 18px 55px #0003}
*{box-sizing:border-box}html{scroll-behavior:smooth;scroll-padding-top:90px}body{margin:0;background:var(--bg);color:var(--text);font-family:Tahoma,Arial,sans-serif;line-height:1.8}a{color:inherit;text-decoration:none}button,input,textarea{font:inherit}.container{width:min(1120px,calc(100% - 40px));margin-inline:auto}.skip-link{position:absolute;top:-60px;right:12px;background:var(--primary);color:white;padding:8px 14px;z-index:20}.skip-link:focus{top:12px}.site-header{position:sticky;top:0;z-index:10;background:var(--bg);border-bottom:1px solid var(--line)}.nav-wrap,.primary-nav,.brand{display:flex;align-items:center}.nav-wrap{min-height:76px;justify-content:space-between;gap:20px}.brand{gap:10px;font-weight:800}.brand-mark{display:grid;place-items:center;width:38px;height:38px;border-radius:12px;background:var(--primary);color:white}.primary-nav{gap:25px;color:var(--muted);font-size:14px}.primary-nav a:hover{color:var(--primary)}.nav-cta{padding:7px 17px;border:1px solid var(--line);border-radius:10px}.icon-button{border:1px solid var(--line);background:var(--card);color:var(--text);border-radius:10px;padding:7px 12px;cursor:pointer}.menu-toggle{display:none}.hero{display:grid;grid-template-columns:1.1fr .9fr;align-items:center;gap:65px;padding-block:90px}.eyebrow{display:inline-block;color:var(--primary);font-weight:700;font-size:13px}.hero h1{font-size:clamp(36px,5vw,60px);line-height:1.35;margin:14px 0}.hero h1 span{color:var(--primary)}.hero-lead,.section-heading>p,.about-panel>p,.contact-panel>div>p{color:var(--muted)}.hero-lead{font-size:17px;max-width:600px}.hero-actions{display:flex;gap:12px;flex-wrap:wrap;margin-top:28px}.button{display:inline-flex;justify-content:center;align-items:center;padding:10px 22px;border-radius:12px;border:1px solid transparent;font-weight:700;cursor:pointer}.button-primary{background:var(--primary);color:white}.button-secondary{border-color:var(--line);background:var(--card)}.trust-list{display:flex;gap:20px;flex-wrap:wrap;padding:0;margin-top:26px;color:var(--muted);font-size:12px;list-style:none}.trust-list li:before{content:'✓';color:var(--primary);margin-left:6px}.hero-art{min-height:340px;display:grid;place-items:center;position:relative;border-radius:32px;background:radial-gradient(circle at 25% 20%,#9dbaff88,transparent 42%),linear-gradient(135deg,#dce8ff,#f4f7ff)}.art-window{width:75%;padding:22px;border-radius:18px;background:#fff;box-shadow:0 24px 70px #1b376333}.window-dots{display:flex;gap:6px;margin-bottom:25px}.window-dots i{width:8px;height:8px;border-radius:50%;background:#b7c8e7}.art-line{height:12px;width:60%;margin:14px 0;border-radius:20px;background:#dfe8f7}.art-line.wide{width:90%;height:18px;background:#9cb9f8}.art-line.short{width:38%}.art-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin-top:24px}.art-grid span{height:72px;border-radius:10px;background:#e8effc}.floating-chip{position:absolute;background:#fff;color:#24457e;padding:8px 14px;border-radius:12px;box-shadow:var(--shadow);font-size:12px;font-weight:bold}.chip-top{top:35px;right:15px}.chip-bottom{bottom:35px;left:15px}.section{padding-block:80px}.section-soft{background:var(--surface)}.section-heading{text-align:center;margin-bottom:36px}.section-heading h2,.about-panel h2,.contact-panel h2{font-size:clamp(25px,3vw,36px);line-height:1.4;margin:8px 0}.feature-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:16px}.feature-card{padding:24px;border:1px solid var(--line);border-radius:18px;background:var(--card)}.feature-icon{display:grid;place-items:center;width:44px;height:44px;border-radius:13px;background:var(--surface);color:var(--primary);font-size:22px}.feature-card h3{margin:14px 0 4px}.feature-card p{color:var(--muted);font-size:14px;margin:0}.about-panel,.contact-panel{display:grid;grid-template-columns:1fr 1fr;gap:40px;align-items:center;padding:35px;border:1px solid var(--line);border-radius:22px;background:var(--card)}.faq-wrap{max-width:850px}.faq-wrap details{padding:17px 20px;margin:10px 0;border:1px solid var(--line);border-radius:13px;background:var(--card)}.faq-wrap summary{font-weight:700;cursor:pointer}.faq-wrap details p{color:var(--muted);margin-bottom:0}.contact-panel form{display:grid;gap:8px}.contact-panel label{font-size:13px;font-weight:bold}.contact-panel input,.contact-panel textarea{width:100%;padding:10px 12px;border:1px solid var(--line);border-radius:10px;background:var(--bg);color:var(--text)}.form-status{min-height:25px;color:var(--primary);font-size:13px}.site-footer{border-top:1px solid var(--line);padding:22px 0;color:var(--muted);font-size:13px}.footer-inner{display:flex;justify-content:space-between;gap:15px}@media(max-width:850px){.hero{gap:30px;padding-block:60px}.feature-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:640px){.container{width:min(100% - 28px,1120px)}.nav-wrap{min-height:66px}.menu-toggle{display:block}.primary-nav{display:none;position:absolute;top:66px;right:14px;left:14px;flex-direction:column;align-items:stretch;padding:16px;border:1px solid var(--line);border-radius:14px;background:var(--card)}.primary-nav.is-open{display:flex}.hero{grid-template-columns:1fr;padding-block:45px}.hero-art{min-height:250px}.feature-grid,.about-panel,.contact-panel{grid-template-columns:1fr}.section{padding-block:55px}.about-panel,.contact-panel{padding:22px}.trust-list{gap:10px}.footer-inner{flex-direction:column}}@media(prefers-reduced-motion:reduce){*,*::before,*::after{scroll-behavior:auto!important;transition:none!important;animation:none!important}}
""",
                "assets/js/app.js" to """(() => {
  const root = document.documentElement;
  const menu = document.querySelector('.menu-toggle');
  const nav = document.querySelector('#primary-nav');
  const theme = document.querySelector('.theme-toggle');
  try {
    const saved = localStorage.getItem('site-theme');
    if (saved === 'dark' || saved === 'light') root.dataset.theme = saved;
  } catch (_) { /* Private browsing may block storage. */ }
  theme?.addEventListener('click', () => {
    const next = root.dataset.theme === 'dark' ? 'light' : 'dark';
    root.dataset.theme = next;
    try { localStorage.setItem('site-theme', next); } catch (_) { /* Optional preference. */ }
  });
  menu?.addEventListener('click', () => {
    const open = menu.getAttribute('aria-expanded') !== 'true';
    menu.setAttribute('aria-expanded', String(open));
    nav?.classList.toggle('is-open', open);
  });
  nav?.querySelectorAll('a').forEach(link => link.addEventListener('click', () => {
    nav.classList.remove('is-open');
    menu?.setAttribute('aria-expanded', 'false');
  }));
  const year = document.querySelector('#current-year');
  if (year) year.textContent = String(new Date().getFullYear());
  const form = document.querySelector('#contact-form');
  form?.addEventListener('submit', event => {
    event.preventDefault();
    const status = document.querySelector('#form-status');
    if (!form.reportValidity()) {
      status.textContent = 'لطفاً فیلدها را درست و کامل وارد کنید.';
      return;
    }
    status.textContent = 'اعتبارسنجی موفق بود. برای ارسال واقعی، فرم را به سرویس امن متصل کنید.';
    form.reset();
  });
})();
""",
                "404.html" to """<!doctype html><html lang="fa" dir="rtl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>صفحه پیدا نشد</title><link rel="stylesheet" href="assets/css/styles.css"></head><body><main class="container section"><span class="eyebrow">خطای 404</span><h1>این صفحه پیدا نشد.</h1><p>نشانی واردشده معتبر نیست یا صفحه جابه‌جا شده است.</p><a class="button button-primary" href="/">بازگشت به صفحه اصلی</a></main></body></html>""",
                "assets/favicon.svg" to """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><rect width="64" height="64" rx="16" fill="#315ee8"/><path d="M17 14h10l12 36H29zM35 14h12L35 50H24z" fill="#fff"/></svg>""",
                "assets/og-cover.svg" to """<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="630" viewBox="0 0 1200 630"><rect width="1200" height="630" fill="#101827"/><circle cx="980" cy="120" r="280" fill="#315ee8" opacity=".35"/><text x="600" y="280" fill="#fff" font-size="68" text-anchor="middle" font-family="Arial">$safeTitle</text><text x="600" y="370" fill="#9dbaff" font-size="34" text-anchor="middle" font-family="Arial">Responsive website starter</text></svg>""",
                "site.webmanifest" to """{"name":"$safeTitle","short_name":"$slug","start_url":"/","display":"standalone","background_color":"#ffffff","theme_color":"#101827","icons":[]}""",
                "robots.txt" to "User-agent: *\nAllow: /\nSitemap: https://example.com/sitemap.xml\n",
                "sitemap.xml" to """<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"><url><loc>https://example.com/</loc></url></urlset>
"""
            )
            "web-react" -> mapOf(
                "README.md" to """# $projectName

React + Vite starter with pinned package versions.

Requirements: Node.js 20.19+ or 22.12+, npm 10+.

Commands:
- npm install
- npm run dev -- --host 0.0.0.0
- npm run build
- npm run preview -- --host 0.0.0.0

The studio in-app preview runs the single-file src/App.jsx starter with pinned React 18.3.1 and Babel. Full Vite dev server, npm plugins and multi-file module graphs still require Node.js. Use npm install, npm run dev and npm run build for the full project.
""",
                "package.json" to """{
  "name": "$slug", "private": true, "version": "1.0.0", "type": "module",
  "scripts": {"dev": "vite", "build": "vite build", "preview": "vite preview"},
  "dependencies": {"react": "18.3.1", "react-dom": "18.3.1"},
  "devDependencies": {"@vitejs/plugin-react": "4.3.4", "vite": "6.1.0"}
}""",
                "vite.config.js" to "import { defineConfig } from 'vite';\nimport react from '@vitejs/plugin-react';\nexport default defineConfig({ plugins: [react()], base: './' });\n",
                "index.html" to """<!doctype html>
<html lang="fa" dir="rtl"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><meta name="theme-color" content="#101827"><meta name="description" content="وب‌اپ React ساخته‌شده با YASIN AI APP STUDIO"><title>$safeTitle</title></head>
<body><div id="root"></div><script type="module" src="/src/main.jsx"></script></body></html>
""",
                "src/main.jsx" to "import React from 'react';\nimport { createRoot } from 'react-dom/client';\nimport App from './App.jsx';\nimport './style.css';\n\ncreateRoot(document.getElementById('root')).render(<React.StrictMode><App /></React.StrictMode>);\n",
                "src/App.jsx" to """import { useState } from 'react';

export default function App() {
  const [count, setCount] = useState(0);
  return (
    <main className="app">
      <span className="eyebrow">React + Vite</span>
      <h1>$safeTitle</h1>
      <p>پروژه React آماده توسعه است.</p>
      <button type="button" onClick={() => setCount(value => value + 1)}>
        تعداد کلیک: {count}
      </button>
    </main>
  );
}
""",
                "src/style.css" to " *{box-sizing:border-box}body{margin:0;min-width:320px;min-height:100vh;background:#f3f6fb;color:#17243a;font-family:Tahoma,Arial,sans-serif}.app{width:min(760px,calc(100% - 32px));margin:12vh auto;padding:40px;background:white;border-radius:20px;box-shadow:0 16px 55px #17243a16}.eyebrow{color:#315ee8;font-weight:700}h1{font-size:clamp(28px,5vw,48px)}p{color:#61718a}button{border:0;border-radius:10px;padding:12px 20px;background:#315ee8;color:white;font:inherit;cursor:pointer}\n".trimStart(),
                ".gitignore" to "node_modules/\ndist/\n.DS_Store\n.env\n.env.*\n!.env.example\n",
                ".env.example" to "# Public, non-secret Vite variables only.\n# Never put private API keys in VITE_ variables.\n"
            )
            "android-kotlin" -> mapOf(
                "README.md" to """# $projectName

Android application starter using Kotlin, Gradle Kotlin DSL and Jetpack Compose.

Requirements:
- JDK 17
- Android Studio with Android SDK Platform 35 and Build Tools 35.0.0
- Gradle 8.11.1

Open this folder in Android Studio, allow Gradle sync, install missing SDK components, then run the app configuration on an emulator or device.

Build APK: Build > Build Bundle(s) / APK(s) > Build APK(s).
Build AAB: Build > Generate Signed Bundle / APK > Android App Bundle. Create and protect your own signing key.

Package: $appId
Minimum Android: API 26
Compile/target SDK: 35
UI: Jetpack Compose Material 3
Unit tests: gradle testDebugUnitTest

This is a complete source starter, not a prebuilt APK. It does not contain Android SDK files or a signing key.

## GitHub build outputs
After pushing to the main or master branch (or starting the workflow manually), GitHub Actions runs unit tests and uploads a debug APK. A signed release APK and AAB are also built when all four repository Actions secrets are configured:
- ANDROID_KEYSTORE_BASE64: base64 of your private .jks/.keystore file
- ANDROID_KEY_ALIAS
- ANDROID_KEY_PASSWORD
- ANDROID_STORE_PASSWORD

Add them in GitHub repository Settings > Secrets and variables > Actions. Never commit the keystore or passwords. Keep a secure offline backup: losing the signing key can prevent updates to the published app.

The workflow deliberately skips signed release outputs when signing secrets are absent; it never creates or publishes a fake/unsigned release bundle.
""",
                "settings.gradle.kts" to """pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "$safeTitle"
include(":app")
""",
                "build.gradle.kts" to """plugins {
    id("com.android.application") version "8.9.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
""",
                "gradle.properties" to "org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8\norg.gradle.parallel=true\nandroid.useAndroidX=true\nkotlin.code.style=official\n",
                "gradle/wrapper/gradle-wrapper.properties" to "distributionBase=GRADLE_USER_HOME\ndistributionPath=wrapper/dists\ndistributionUrl=https\\://services.gradle.org/distributions/gradle-8.11.1-bin.zip\nnetworkTimeout=10000\nvalidateDistributionUrl=true\nzipStoreBase=GRADLE_USER_HOME\nzipStorePath=wrapper/dists\n",
                ".gitignore" to "*.iml\n.gradle/\n.idea/\nlocal.properties\n.DS_Store\n/build/\n**/build/\ncaptures/\n.externalNativeBuild/\n.cxx/\n*.jks\n*.keystore\nsecrets.properties\n",
                ".github/workflows/android.yml" to """name: Android APK and AAB Build
on:
  push:
    branches: [ main, master ]
  pull_request:
  workflow_dispatch:
permissions:
  contents: read
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.11.1'
      - uses: android-actions/setup-android@v4
        with:
          packages: 'platform-tools'
      - name: Install Android SDK
        run: sdkmanager "platforms;android-35" "build-tools;35.0.0"
      - name: Run unit tests
        run: gradle --no-daemon testDebugUnitTest
      - name: Build debug APK
        run: gradle --no-daemon assembleDebug
      - uses: actions/upload-artifact@v4
        with:
          name: android-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
          if-no-files-found: error
      - name: Prepare release signing key
        id: signing
        env:
          ANDROID_KEYSTORE_BASE64: $dollar{{ secrets.ANDROID_KEYSTORE_BASE64 }}
          ANDROID_KEY_ALIAS: $dollar{{ secrets.ANDROID_KEY_ALIAS }}
          ANDROID_KEY_PASSWORD: $dollar{{ secrets.ANDROID_KEY_PASSWORD }}
          ANDROID_STORE_PASSWORD: $dollar{{ secrets.ANDROID_STORE_PASSWORD }}
        run: |
          if [ -z "$dollar{ANDROID_KEYSTORE_BASE64}" ] || [ -z "$dollar{ANDROID_KEY_ALIAS}" ] || [ -z "$dollar{ANDROID_KEY_PASSWORD}" ] || [ -z "$dollar{ANDROID_STORE_PASSWORD}" ]; then
            echo "configured=false" >> "$dollar{GITHUB_OUTPUT}"
            echo "::notice::Signing secrets are missing; signed APK/AAB generation was skipped."
            exit 0
          fi
          printf '%s' "$dollar{ANDROID_KEYSTORE_BASE64}" | base64 --decode > "$dollar{RUNNER_TEMP}/release.keystore"
          echo "configured=true" >> "$dollar{GITHUB_OUTPUT}"
      - name: Build signed release APK and AAB
        if: steps.signing.outputs.configured == 'true'
        env:
          ANDROID_KEYSTORE_FILE: $dollar{{ runner.temp }}/release.keystore
          ANDROID_KEY_ALIAS: $dollar{{ secrets.ANDROID_KEY_ALIAS }}
          ANDROID_KEY_PASSWORD: $dollar{{ secrets.ANDROID_KEY_PASSWORD }}
          ANDROID_STORE_PASSWORD: $dollar{{ secrets.ANDROID_STORE_PASSWORD }}
        run: gradle --no-daemon assembleRelease bundleRelease
      - name: Upload signed release APK and AAB
        if: steps.signing.outputs.configured == 'true'
        uses: actions/upload-artifact@v4
        with:
          name: android-signed-release
          path: |
            app/build/outputs/apk/release/*.apk
            app/build/outputs/bundle/release/*.aab
          if-no-files-found: error
""",
                "app/build.gradle.kts" to """plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "$appId"
    compileSdk = 35
    defaultConfig {
        applicationId = "$appId"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
val releaseStoreFile = System.getenv("ANDROID_KEYSTORE_FILE")
android {
    signingConfigs {
        create("release") {
            if (!releaseStoreFile.isNullOrBlank()) {
                storeFile = file(releaseStoreFile)
                storePassword = System.getenv("ANDROID_STORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
""",
                "app/proguard-rules.pro" to "# Add project-specific R8 rules here when release minification is enabled.\n",
                "app/src/main/AndroidManifest.xml" to """<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:allowBackup="false" android:label="$safeTitle" android:supportsRtl="true" android:theme="@style/Theme.GeneratedApp">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>
        </activity>
    </application>
</manifest>
""",
                "app/src/main/java/$packagePath/MainActivity.kt" to """package $appId

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import $appId.ui.theme.GeneratedAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeneratedAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    StarterScreen()
                }
            }
        }
    }
}

@Composable
private fun StarterScreen() {
    var taps by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("$safeTitle", style = MaterialTheme.typography.headlineMedium)
        Text("پروژه اندروید شما آماده توسعه است.")
        Button(onClick = { taps++ }) { Text("تعداد لمس: " + taps) }
    }
}
""",
                "app/src/main/java/$packagePath/ui/theme/Theme.kt" to """package $appId.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF315EE8),
    secondary = Color(0xFF9A6B00),
    background = Color(0xFFF7F8FC),
    surface = Color.White
)

@Composable
fun GeneratedAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
""",
                "app/src/main/res/values/strings.xml" to "<resources><string name=\"app_name\">$safeTitle</string></resources>\n",
                "app/src/main/res/values/styles.xml" to """<resources>
    <style name="Theme.GeneratedApp" parent="android:style/Theme.Material.Light.NoActionBar">
        <item name="android:fontFamily">sans</item>
        <item name="android:colorAccent">#315EE8</item>
        <item name="android:windowLightStatusBar">true</item>
    </style>
</resources>
""",
                "app/src/test/java/$packagePath/ExampleUnitTest.kt" to """package $appId

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test fun arithmeticWorks() {
        assertEquals(4, 2 + 2)
    }
}
"""
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
