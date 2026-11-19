package com.yasin.aiappstudio

data class AiUsage(val promptTokens: Int, val completionTokens: Int, val totalTokens: Int)

object AiUsageParser {
    fun parse(responseJson: String): AiUsage? {
        fun token(name: String): Int? =
            Regex("\"$name\"\\s*:\\s*(\\d+)").find(responseJson)?.groupValues?.get(1)?.toIntOrNull()
        val prompt = token("prompt_tokens") ?: return null
        val completion = token("completion_tokens") ?: return null
        val total = token("total_tokens") ?: return null
        return AiUsage(prompt, completion, total)
    }
}
