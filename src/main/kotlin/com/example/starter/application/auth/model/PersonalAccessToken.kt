package com.example.starter.application.auth.model

import java.time.Instant

data class PersonalAccessToken(
    val id: Long,
    val userId: Long,
    val name: String,
    val abilities: Set<String>,
    val lastUsedAt: Instant?,
    val expiresAt: Instant?,
    val revokedAt: Instant?,
    val createdAt: Instant,
) {
    fun can(ability: String): Boolean = "*" in abilities || ability in abilities
}

data class NewAccessToken(
    val accessToken: PersonalAccessToken,
    val plainTextToken: String,
)
