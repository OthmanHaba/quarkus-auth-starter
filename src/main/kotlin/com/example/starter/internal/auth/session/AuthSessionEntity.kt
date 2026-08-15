package com.example.starter.internal.auth.session

import com.example.starter.application.auth.model.AuthSession
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "auth_sessions")
class AuthSessionEntity : PanacheEntityBase {
    @Id
    var id: UUID = UUID.randomUUID()

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0

    @Column(name = "session_hash", nullable = false, length = 64)
    var sessionHash: String = ""

    @Column(name = "ip_address", length = 64)
    var ipAddress: String? = null

    @Column(name = "user_agent")
    var userAgent: String? = null

    @Column(name = "last_used_at", nullable = false)
    var lastUsedAt: Instant = Instant.now()

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now()

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    fun toModel() = AuthSession(
        id = id,
        userId = userId,
        ipAddress = ipAddress,
        userAgent = userAgent,
        lastUsedAt = lastUsedAt,
        expiresAt = expiresAt,
        revokedAt = revokedAt,
        createdAt = createdAt,
    )
}
