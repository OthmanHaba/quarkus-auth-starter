package com.example.starter.internal.auth.token

data class ParsedPersonalToken(val id: Long, val secret: String) {
    companion object {
        fun parse(value: String): ParsedPersonalToken? {
            val parts = value.split('|', limit = 2)
            val id = parts.getOrNull(0)?.toLongOrNull()?.takeIf { it > 0 } ?: return null
            val secret = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
            return ParsedPersonalToken(id, secret)
        }
    }
}
