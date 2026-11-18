package com.yasin.aiappstudio

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Local-first project workspace. All paths are validated against the workspace root.
 * This class deliberately does not execute project code; builds run in a separate,
 * isolated build service.
 */
class WorkspaceManager(private val root: File) {
    data class Entry(val path: String, val directory: Boolean, val size: Long)
    data class Project(val name: String, val updatedAt: Long, val fileCount: Int)

    init {
        check(root.exists() || root.mkdirs()) { "ساخت پوشه فضای کاری ممکن نشد." }
        check(root.isDirectory) { "مسیر فضای کاری پوشه نیست." }
    }

    fun listProjects(): List<Project> = root.listFiles()
        ?.filter { it.isDirectory && !it.name.startsWith(".") }
        ?.map { dir ->
            Project(dir.name, dir.lastModified(),
                dir.walkTopDown().count { it.isFile && !it.relativeTo(dir).path.startsWith(".history") })
        }?.sortedByDescending { it.updatedAt } ?: emptyList()

    fun createProject(name: String, templateId: String = "blank"): File {
        val clean = validateProjectName(name)
        val target = File(root, clean)
        require(!target.exists()) { "پروژه‌ای با این نام وجود دارد." }
        check(target.mkdirs()) { "ساخت پروژه ناموفق بود." }
        try {
            ProjectTemplates.files(templateId, clean).forEach { (path, body) ->
                val file = child(target, path) ?: error("مسیر قالب نامعتبر است.")
                check(file.parentFile?.mkdirs() != false)
                file.writeText(body)
            }
        } catch (e: Exception) {
            target.deleteRecursively()
            throw e
        }
        return target
    }

    fun renameProject(oldName: String, newName: String) {
        val source = project(oldName)
        val clean = validateProjectName(newName)
        val target = File(root, clean)
        require(!target.exists()) { "نام مقصد از قبل وجود دارد." }
        check(source.renameTo(target)) { "تغییر نام پروژه انجام نشد." }
    }

    fun duplicateProject(name: String, copyName: String): File {
        val source = project(name)
        val clean = validateProjectName(copyName)
        val target = File(root, clean)
        require(!target.exists()) { "نام مقصد از قبل وجود دارد." }
        source.copyRecursively(target, overwrite = false) { _, file ->
            if (file.name == ".history") OnErrorAction.SKIP else OnErrorAction.TERMINATE
        }
        return target
    }

    fun deleteProject(name: String) {
        val dir = project(name)
        check(dir.deleteRecursively()) { "حذف کامل پروژه ناموفق بود." }
    }

    fun listEntries(name: String): List<Entry> {
        val dir = project(name)
        return dir.walkTopDown().filter { it != dir && !it.relativeTo(dir).path.split(File.separator).any { p -> p.startsWith(".") } }
            .map { Entry(it.relativeTo(dir).invariantSeparatorsPath, it.isDirectory, if (it.isFile) it.length() else 0L) }
            .sortedWith(compareBy<Entry>({ !it.directory }, { it.path.lowercase() })).toList()
    }

    fun readFile(name: String, relativePath: String): String {
        val file = child(project(name), relativePath) ?: error("مسیر فایل نامعتبر است.")
        require(file.isFile) { "فایل پیدا نشد." }
        require(file.length() <= MAX_TEXT_BYTES) { "فایل برای ویرایش متنی بیش از حد بزرگ است." }
        return file.readText()
    }

    fun saveFile(name: String, relativePath: String, content: String) {
        require(content.toByteArray(Charsets.UTF_8).size <= MAX_TEXT_BYTES) { "حجم فایل بیش از حد مجاز است." }
        val dir = project(name)
        val file = child(dir, relativePath) ?: error("مسیر فایل نامعتبر است.")
        require(relativePath.isNotBlank() && !relativePath.endsWith("/")) { "نام فایل معتبر نیست." }
        check(file.parentFile?.mkdirs() != false)
        if (file.exists() && file.isFile) {
            val history = File(dir, ".history").apply { mkdirs() }
            val safe = relativePath.replace('/', '_').replace('\\', '_')
            File(history, "${System.currentTimeMillis()}-$safe.snapshot").writeText(file.readText())
        }
        file.writeText(content)
    }

    fun createEntry(name: String, relativePath: String, directory: Boolean) {
        val target = child(project(name), relativePath) ?: error("مسیر نامعتبر است.")
        require(relativePath.isNotBlank())
        require(!target.exists()) { "این فایل یا پوشه از قبل وجود دارد." }
        check(target.parentFile?.mkdirs() != false)
        val ok = if (directory) target.mkdirs() else target.createNewFile()
        check(ok) { "ساخت فایل یا پوشه ناموفق بود." }
    }

    fun renameEntry(name: String, oldPath: String, newName: String) {
        require(newName.isNotBlank() && !newName.contains('/') && !newName.contains('\\'))
        val source = child(project(name), oldPath) ?: error("مسیر مبدا نامعتبر است.")
        require(source.exists() && source != project(name))
        val target = File(source.parentFile, newName)
        require(!target.exists()) { "نام مقصد وجود دارد." }
        check(source.renameTo(target)) { "تغییر نام ناموفق بود." }
    }

    fun deleteEntry(name: String, relativePath: String) {
        val target = child(project(name), relativePath) ?: error("مسیر نامعتبر است.")
        require(target != project(name) && target.exists())
        check(target.deleteRecursively()) { "حذف ناموفق بود." }
    }

    fun search(name: String, query: String, maxResults: Int = 100): List<Pair<String, Int>> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val dir = project(name)
        val hits = mutableListOf<Pair<String, Int>>()
        dir.walkTopDown().filter { it.isFile && !it.relativeTo(dir).path.split(File.separator).any { p -> p.startsWith(".") } }
            .forEach { file ->
                if (hits.size >= maxResults || file.length() > MAX_TEXT_BYTES) return@forEach
                runCatching { file.useLines { lines -> lines.forEachIndexed { i, line ->
                    if (hits.size < maxResults && line.contains(q, ignoreCase = true))
                        hits += "${file.relativeTo(dir).invariantSeparatorsPath}:${i + 1}" to line.trim().take(240)
                } } }
            }
        return hits
    }

    fun exportProject(name: String, destination: File) {
        val dir = project(name)
        ZipOutputStream(BufferedOutputStream(FileOutputStream(destination))).use { zip ->
            dir.walkTopDown().filter { it.isFile && it.name != ".DS_Store" &&
                !it.relativeTo(dir).path.split(File.separator).any { p -> p == ".history" } }
                .forEach { file ->
                    val path = file.relativeTo(dir).invariantSeparatorsPath
                    zip.putNextEntry(ZipEntry(path))
                    BufferedInputStream(FileInputStream(file)).use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
        }
    }

    fun importProject(source: File, requestedName: String): File {
        val name = validateProjectName(requestedName)
        val target = File(root, name)
        require(!target.exists()) { "پروژه‌ای با این نام وجود دارد." }
        check(target.mkdirs()) { "ساخت پوشه پروژه ناموفق بود." }
        var total = 0L
        var entries = 0
        try {
            ZipInputStream(BufferedInputStream(FileInputStream(source))).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entries++
                    require(entries <= MAX_ZIP_ENTRIES) { "تعداد فایل‌های ZIP بیش از حد مجاز است." }
                    val out = child(target, entry.name) ?: error("ZIP شامل مسیر غیرمجاز است.")
                    if (entry.isDirectory) {
                        check(out.mkdirs() || out.isDirectory)
                    } else {
                        check(out.parentFile?.mkdirs() != false)
                        var written = 0L
                        FileOutputStream(out).use { output ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                written += count
                                total += count
                                require(written <= MAX_ENTRY_BYTES && total <= MAX_IMPORT_BYTES) { "حجم فایل ZIP بیش از حد مجاز است." }
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
            return target
        } catch (e: Exception) {
            target.deleteRecursively()
            throw e
        }
    }

    private fun project(name: String): File {
        val clean = validateProjectName(name)
        return File(root, clean).canonicalFile.also { require(it.isDirectory && it.parentFile == root.canonicalFile) { "پروژه پیدا نشد." } }
    }

    private fun child(base: File, relative: String): File? {
        if (relative.isBlank() || relative.startsWith("/") || relative.contains('\\')) return null
        val canonicalBase = base.canonicalFile
        val target = File(canonicalBase, relative).canonicalFile
        return target.takeIf { it.path.startsWith(canonicalBase.path + File.separator) }
    }

    private fun validateProjectName(value: String): String {
        val name = value.trim()
        require(name.isNotEmpty() && name.length <= 70 && name != "." && name != "..")
        require(name.none { it == '/' || it == '\\' || it.isISOControl() })
        require(!name.startsWith("."))
        return name
    }

    companion object {
        private const val MAX_TEXT_BYTES = 2 * 1024 * 1024L
        private const val MAX_ENTRY_BYTES = 25 * 1024 * 1024L
        private const val MAX_IMPORT_BYTES = 100 * 1024 * 1024L
        private const val MAX_ZIP_ENTRIES = 5000
    }
}
