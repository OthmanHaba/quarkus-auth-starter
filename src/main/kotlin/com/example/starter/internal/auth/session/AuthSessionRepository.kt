package com.example.starter.internal.auth.session

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class AuthSessionRepository : PanacheRepositoryBase<AuthSessionEntity, UUID> {
    fun listForUser(userId: Long): List<AuthSessionEntity> =
        list("userId = ?1 order by createdAt desc", userId)

    fun findForUser(userId: Long, sessionId: UUID): AuthSessionEntity? =
        find("userId = ?1 and id = ?2", userId, sessionId).firstResult()

    fun deleteExpiredOrOldRevoked(now: Instant, revokedBefore: Instant): Long =
        delete("expiresAt < ?1 or (revokedAt is not null and revokedAt < ?2)", now, revokedBefore)
}
