package com.example.starter.application.auth.model

import java.time.Instant
import java.util.UUID

data class AuthSession(
    val id: UUID,
    val userId: Long,
    val ipAddress: String?,
    val userAgent: String?,
    val lastUsedAt: Instant,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val createdAt: Instant,
)

data class NewAuthSession(
    val session: AuthSession,
    val plainTextSession: String,
)
