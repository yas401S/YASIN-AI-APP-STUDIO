package com.yasin.aiappstudio

import org.junit.Assert.*
import org.junit.Test

class AiUsageParserTest {
    @Test fun readsProviderReportedTokenCounts() {
        val usage = AiUsageParser.parse("""{"usage":{"prompt_tokens":12,"completion_tokens":8,"total_tokens":20}}""")
        assertEquals(AiUsage(12, 8, 20), usage)
    }

    @Test fun doesNotInventUsageWhenProviderOmitsIt() {
        assertNull(AiUsageParser.parse("""{"choices":[]}"""))
    }
}
