package com.yasin.aiappstudio

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts locally configured provider secrets with a non-exportable Android Keystore key.
 * This protects persisted storage; it is not a substitute for a server-side provider proxy.
 */
object SecureSecretStore {
    private const val ALIAS = "yasin-ai-studio-provider-key-v1"
    private const val PREFS = "studio_secure"
    private const val CIPHER = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    fun read(context: Context): String {
        val securePrefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val encoded = securePrefs.getString("provider_key", null)
        if (!encoded.isNullOrBlank()) return runCatching { decrypt(encoded) }.getOrDefault("")
        // One-time migration from the old plaintext preference.
        val legacyPrefs = context.getSharedPreferences("studio", Context.MODE_PRIVATE)
        val legacy = legacyPrefs.getString("key", "").orEmpty()
        if (legacy.isNotBlank()) {
            write(context, legacy)
            legacyPrefs.edit().remove("key").apply()
        }
        return legacy
    }

    fun write(context: Context, secret: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (secret.isBlank()) {
            prefs.edit().remove("provider_key").apply()
            return
        }
        val cipher = Cipher.getInstance(CIPHER)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(secret.toByteArray(Charsets.UTF_8))
        val payload = ByteArrayOutputStream().apply {
            write(cipher.iv)
            write(encrypted)
        }.toByteArray()
        prefs.edit().putString("provider_key", Base64.encodeToString(payload, Base64.NO_WRAP)).apply()
    }

    private fun decrypt(encoded: String): String {
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        require(payload.size > IV_BYTES) { "کلید ذخیره‌شده معتبر نیست." }
        val iv = payload.copyOfRange(0, IV_BYTES)
        val encrypted = payload.copyOfRange(IV_BYTES, payload.size)
        val cipher = Cipher.getInstance(CIPHER)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
