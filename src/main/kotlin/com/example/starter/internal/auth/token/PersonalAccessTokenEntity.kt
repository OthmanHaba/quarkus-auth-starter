package com.example.starter.internal.auth.token

import com.example.starter.application.auth.model.PersonalAccessToken
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "personal_access_tokens")
class PersonalAccessTokenEntity : PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0

    @Column(nullable = false)
    var name: String = ""

    @Column(name = "token_hash", nullable = false, length = 64)
    var tokenHash: String = ""

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    var abilities: MutableSet<String> = mutableSetOf()

    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null

    @Column(name = "expires_at")
    var expiresAt: Instant? = null

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    fun toModel() = PersonalAccessToken(
        id = requireNotNull(id),
        userId = userId,
        name = name,
        abilities = abilities.toSet(),
        lastUsedAt = lastUsedAt,
        expiresAt = expiresAt,
        revokedAt = revokedAt,
        createdAt = createdAt,
    )
}
