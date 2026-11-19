package com.yasin.aiappstudio

import org.junit.Assert.*
import org.junit.Test

class DiagnosticEngineTest {
    @Test fun identifiesAndRepairsOnlySafeWhitespaceIssues() {
        val source = "fun main() {  \n\tprintln(\"hi\")\t"
        val report = DiagnosticEngine.repairSafeFormatting(source)
        assertEquals(3, report.issues.size)
        assertEquals(3, report.fixedCount)
        assertEquals("fun main() {\n\tprintln(\"hi\")\n", report.content)
        assertTrue(DiagnosticEngine.inspect(report.content).isEmpty())
    }

    @Test fun leavesCleanAndEmptyFilesUnchanged() {
        assertEquals("", DiagnosticEngine.repairSafeFormatting("").content)
        val clean = "val answer = 42\n"
        assertEquals(clean, DiagnosticEngine.repairSafeFormatting(clean).content)
    }
    @Test fun detectsUnbalancedDelimitersButIgnoresStringsAndComments() {
        val source = """fun main() {
    val text = "this ) is not code"
    // this } is a comment
    println(text
}
"""
        val issues = DiagnosticEngine.inspect("Main.kt", source)
        assertTrue(issues.any { it.message.contains("بسته نشده") })
        assertFalse(issues.any { it.message.contains("نامتطابق") })
    }

    @Test fun detectsMismatchedDelimitersAndMergeConflictMarkers() {
        val source = """const value = [1, 2);
<<<<<<< branch-a
=======
>>>>>>> branch-b
"""
        val issues = DiagnosticEngine.inspect("main.js", source)
        assertTrue(issues.any { it.message.contains("نامتطابق") })
        assertEquals(3, issues.count { it.message.contains("تعارض Git") })
    }

    @Test fun doesNotRunDelimiterChecksOnPlainTextFiles() {
        val issues = DiagnosticEngine.inspect("notes.txt", "text with ( unmatched")
        assertFalse(issues.any { it.message.contains("بسته نشده") })
    }

}
