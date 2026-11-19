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
 * Conservative local source hygiene checks. It never guesses at program semantics.
 * Only trailing whitespace and a missing final newline are changed automatically.
 */
object DiagnosticEngine {
    fun inspect(source: String): List<CodeIssue> {
        val lines = source.split("\n")
        val issues = mutableListOf<CodeIssue>()
        lines.forEachIndexed { index, line ->
            if (line.endsWith(" ") || line.endsWith("\t")) {
                issues += CodeIssue(index + 1, "فاصله اضافی در انتهای خط", true)
            }
        }
        if (source.isNotEmpty() && !source.endsWith("\n")) {
            issues += CodeIssue(lines.size, "فایل با خط جدید پایان نمی‌یابد", true)
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
