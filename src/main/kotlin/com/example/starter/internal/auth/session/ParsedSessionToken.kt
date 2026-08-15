package com.example.starter.internal.auth.session

import java.util.UUID

data class ParsedSessionToken(val id: UUID, val secret: String) {
    companion object {
        fun parse(value: String): ParsedSessionToken? {
            val parts = value.split('|', limit = 2)
            val id = runCatching { UUID.fromString(parts.getOrNull(0)) }.getOrNull() ?: return null
            val secret = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
            return ParsedSessionToken(id, secret)
        }
    }
}
