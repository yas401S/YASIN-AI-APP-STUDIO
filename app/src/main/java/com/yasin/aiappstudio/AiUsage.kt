package com.yasin.aiappstudio

import org.json.JSONObject

data class AiUsage(val promptTokens: Int, val completionTokens: Int, val totalTokens: Int)

object AiUsageParser {
    fun parse(response: JSONObject): AiUsage? {
        val usage = response.optJSONObject("usage") ?: return null
        val prompt = usage.optInt("prompt_tokens", -1)
        val completion = usage.optInt("completion_tokens", -1)
        val total = usage.optInt("total_tokens", -1)
        if (prompt < 0 || completion < 0 || total < 0) return null
        return AiUsage(prompt, completion, total)
    }
}
