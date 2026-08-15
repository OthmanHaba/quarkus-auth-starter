package com.example.starter.internal.auth.security

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.HexFormat

object CredentialSecrets {
    private val secureRandom = SecureRandom()

    fun generate(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(secret: String): String = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(StandardCharsets.UTF_8)),
    )

    fun matches(secret: String, expectedHash: String): Boolean {
        val actual = MessageDigest.getInstance("SHA-256")
            .digest(secret.toByteArray(StandardCharsets.UTF_8))
        val expected = runCatching { HexFormat.of().parseHex(expectedHash) }.getOrNull() ?: return false
        return MessageDigest.isEqual(actual, expected)
    }
}
