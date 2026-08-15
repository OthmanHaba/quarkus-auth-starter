package com.example.starter

import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class DatabaseTestSupport(private val entityManager: EntityManager) {
    @Transactional
    fun reset() {
        entityManager.createNativeQuery(
            "TRUNCATE TABLE personal_access_tokens, auth_sessions, users RESTART IDENTITY CASCADE",
        ).executeUpdate()
    }

    @Transactional
    fun promoteToAdmin(email: String) {
        entityManager.createNativeQuery("UPDATE users SET role = 'ADMIN' WHERE email = :email")
            .setParameter("email", email)
            .executeUpdate()
    }

    @Transactional
    fun userId(email: String): Long =
        (entityManager.createNativeQuery("SELECT id FROM users WHERE email = :email")
            .setParameter("email", email)
            .singleResult as Number).toLong()

    @Transactional
    fun deactivateUser(userId: Long) {
        entityManager.createNativeQuery("UPDATE users SET active = FALSE WHERE id = :id")
            .setParameter("id", userId)
            .executeUpdate()
    }

    @Transactional
    fun deleteUser(userId: Long) {
        entityManager.createNativeQuery("DELETE FROM users WHERE id = :id")
            .setParameter("id", userId)
            .executeUpdate()
    }

    @Transactional
    fun tokenHash(tokenId: Long): String =
        entityManager.createNativeQuery("SELECT token_hash FROM personal_access_tokens WHERE id = :id")
            .setParameter("id", tokenId)
            .singleResult as String

    @Transactional
    fun expireToken(tokenId: Long) {
        entityManager.createNativeQuery("UPDATE personal_access_tokens SET expires_at = :expiresAt WHERE id = :id")
            .setParameter("expiresAt", Instant.now().minusSeconds(1))
            .setParameter("id", tokenId)
            .executeUpdate()
    }

    @Transactional
    fun expireSession(sessionId: UUID) {
        entityManager.createNativeQuery("UPDATE auth_sessions SET expires_at = :expiresAt WHERE id = :id")
            .setParameter("expiresAt", Instant.now().minusSeconds(1))
            .setParameter("id", sessionId)
            .executeUpdate()
    }

    @Transactional
    fun isSessionRevoked(sessionId: UUID): Boolean =
        entityManager.createNativeQuery("SELECT revoked_at IS NOT NULL FROM auth_sessions WHERE id = :id")
            .setParameter("id", sessionId)
            .singleResult as Boolean

    @Transactional
    fun sessionLastUsedEpochMillis(sessionId: UUID): Long =
        ((entityManager.createNativeQuery("SELECT EXTRACT(EPOCH FROM last_used_at) * 1000 FROM auth_sessions WHERE id = :id")
            .setParameter("id", sessionId)
            .singleResult as Number).toLong())
}
