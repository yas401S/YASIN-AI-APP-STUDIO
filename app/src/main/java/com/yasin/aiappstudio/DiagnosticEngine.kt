package com.yasin.aiappstudio

data class CodeIssue(
    val line: Int,
    val message: String,
    val automaticallyFixable: Boolean
)

data class RepairReport(
    val content: String,
    val issues: List<CodeIssue>,
    val fixedCount: Int
)

/**
 * Deterministic local diagnostics. It reports only checks it can establish without
 * running a compiler; automatic edits are limited to whitespace and final newline.
 */
object DiagnosticEngine {
    fun inspect(source: String): List<CodeIssue> = inspect("", source)

    fun inspect(path: String, source: String): List<CodeIssue> {
        val lines = source.split("\n")
        val issues = mutableListOf<CodeIssue>()
        lines.forEachIndexed { index, line ->
            if (line.endsWith(" ") || line.endsWith("\t")) {
                issues += CodeIssue(index + 1, "فاصله اضافی در انتهای خط", true)
            }
            if (line.trimStart().startsWith("<<<<<<< ") ||
                line.trimStart().startsWith("=======") ||
                line.trimStart().startsWith(">>>>>>> ")) {
                issues += CodeIssue(index + 1, "نشانگر حل‌نشده تعارض Git", false)
            }
        }
        if (source.isNotEmpty() && !source.endsWith("\n")) {
            issues += CodeIssue(lines.size, "فایل با خط جدید پایان نمی‌یابد", true)
        }

        val extension = path.substringAfterLast('.', "").lowercase()
        if (extension in setOf("kt", "kts", "java", "js", "mjs", "ts", "tsx", "jsx", "css")) {
            issues += delimiterIssues(source)
        }
        return issues.distinct()
    }

    private fun delimiterIssues(source: String): List<CodeIssue> {
        data class Open(val char: Char, val line: Int)
        val stack = ArrayDeque<Open>()
        val issues = mutableListOf<CodeIssue>()
        var line = 1
        var quote: Char? = null
        var escaped = false
        var lineComment = false
        var blockComment = false
        var i = 0

        while (i < source.length) {
            val ch = source[i]
            val next = source.getOrNull(i + 1)
            if (ch == '\n') {
                line++
                lineComment = false
                if (quote == '"' || quote == '\'') quote = null
                escaped = false
                i++
                continue
            }
            if (lineComment) { i++; continue }
            if (blockComment) {
                if (ch == '*' && next == '/') { blockComment = false; i += 2 } else i++
                continue
            }
            if (quote != null) {
                if (escaped) escaped = false
                else if (ch == '\\') escaped = true
                else if (ch == quote) quote = null
                i++
                continue
            }
            if (ch == '/' && next == '/') { lineComment = true; i += 2; continue }
            if (ch == '/' && next == '*') { blockComment = true; i += 2; continue }
            if (ch == '"' || ch == '\'' || ch == '`') { quote = ch; i++; continue }

            when (ch) {
                '(', '[', '{' -> stack.addLast(Open(ch, line))
                ')', ']', '}' -> {
                    val expected = when (ch) { ')' -> '('; ']' -> '['; else -> '{' }
                    if (stack.isEmpty() || stack.last().char != expected) {
                        issues += CodeIssue(line, "بستار نامتطابق «$ch»", false)
                    } else stack.removeLast()
                }
            }
            i++
        }
        stack.forEach { open ->
            issues += CodeIssue(open.line, "بستار «${open.char}» بسته نشده است", false)
        }
        return issues
    }

    fun repairSafeFormatting(source: String): RepairReport {
        val issues = inspect(source)
        val hadFinalNewline = source.endsWith("\n")
        val repairedLines = source.split("\n").map { it.trimEnd(' ', '\t') }
        var repaired = repairedLines.joinToString("\n")
        if (source.isNotEmpty() && !hadFinalNewline) repaired += "\n"
        return RepairReport(repaired, issues, issues.count { it.automaticallyFixable })
    }
}
