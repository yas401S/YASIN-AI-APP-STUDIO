package com.yasin.aiappstudio

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class GitHubDeviceCode(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresInSeconds: Int,
    val intervalSeconds: Int
)

object GitHubDeviceFlow {
    private const val DEVICE_URL = "https://github.com/login/device/code"
    private const val TOKEN_URL = "https://github.com/login/oauth/access_token"

    fun requestDeviceCode(clientId: String): GitHubDeviceCode {
        val body = postForm(DEVICE_URL, mapOf("client_id" to clientId))
        val json = JSONObject(body)
        return GitHubDeviceCode(
            deviceCode = json.getString("device_code"),
            userCode = json.getString("user_code"),
            verificationUri = json.getString("verification_uri"),
            expiresInSeconds = json.optInt("expires_in", 900),
            intervalSeconds = json.optInt("interval", 5)
        )
    }

    fun pollForAccessToken(clientId: String, deviceCode: String): String? {
        val body = postForm(TOKEN_URL, mapOf(
            "client_id" to clientId,
            "device_code" to deviceCode,
            "grant_type" to "urn:ietf:params:oauth:grant-type:device_code"
        ))
        val json = JSONObject(body)
        val token = json.optString("access_token")
        if (token.isNotBlank()) return token
        when (json.optString("error")) {
            "authorization_pending" -> return null
            "slow_down" -> throw GitHubSlowDownException()
            "expired_token" -> throw IllegalStateException("کد ورود منقضی شده است؛ دوباره درخواست کد بدهید.")
            "access_denied" -> throw IllegalStateException("درخواست ورود در GitHub رد شد.")
            else -> throw IllegalStateException(json.optString("error_description", "دریافت مجوز GitHub ناموفق بود."))
        }
    }

    private fun postForm(endpoint: String, values: Map<String, String>): String {
        val encoded = values.entries.joinToString("&") { (key, value) ->
            URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
        }
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("User-Agent", "YASIN-AI-APP-STUDIO")
        }
        return try {
            connection.outputStream.use { it.write(encoded.toByteArray(Charsets.UTF_8)) }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("ارتباط با GitHub ناموفق بود (" + connection.responseCode + ").")
            }
            response
        } finally {
            connection.disconnect()
        }
    }
}

class GitHubSlowDownException : Exception()
