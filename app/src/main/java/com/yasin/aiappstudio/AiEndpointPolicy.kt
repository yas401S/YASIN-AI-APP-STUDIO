package com.yasin.aiappstudio

import java.net.URI

/** Validates provider URLs before an Authorization header is attached. */
object AiEndpointPolicy {
    fun validate(value: String): String {
        val raw = value.trim()
        require(raw.isNotEmpty()) { "آدرس خالی است." }
        val uri = URI(raw)
        require(uri.scheme.equals("https", ignoreCase = true)) { "فقط نشانی HTTPS امن پذیرفته می‌شود." }
        require(!uri.host.isNullOrBlank()) { "نام میزبان معتبر نیست." }
        require(uri.rawUserInfo == null) { "قرار دادن نام کاربری یا رمز در URL مجاز نیست." }
        require(uri.port == -1 || uri.port in 1..65535) { "شماره درگاه نامعتبر است." }
        require(uri.fragment == null) { "نشانی نباید fragment داشته باشد." }
        return uri.toASCIIString()
    }
}
