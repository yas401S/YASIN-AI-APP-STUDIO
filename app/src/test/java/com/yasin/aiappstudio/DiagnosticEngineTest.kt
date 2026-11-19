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
}
