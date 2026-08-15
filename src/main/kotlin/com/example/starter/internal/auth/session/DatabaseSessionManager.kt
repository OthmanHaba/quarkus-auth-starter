package com.example.starter.internal.auth.session

import com.example.starter.application.auth.SessionManager
import com.example.starter.application.auth.model.AuthSession
import com.example.starter.application.auth.model.NewAuthSession
import com.example.starter.internal.auth.config.AuthConfig
import com.example.starter.internal.auth.security.CredentialSecrets
import com.example.starter.shared.ApiException
import com.example.starter.shared.ErrorCode
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class DatabaseSessionManager(
    private val repository: AuthSessionRepository,
    private val config: AuthConfig,
) : SessionManager {
    private val log = Logger.getLogger(DatabaseSessionManager::class.java)

    @Transactional
    override fun create(userId: Long, ipAddress: String?, userAgent: String?): NewAuthSession {
        val secret = CredentialSecrets.generate()
        val now = Instant.now()
        val entity = AuthSessionEntity().apply {
            this.userId = userId
            sessionHash = CredentialSecrets.hash(secret)
            this.ipAddress = ipAddress?.take(64)
            this.userAgent = userAgent?.take(1024)
            lastUsedAt = now
            expiresAt = now.plus(config.sessionDuration())
            createdAt = now
        }
        repository.persist(entity)
        repository.flush()
        log.infof("auth event=session_created user_id=%d session_id=%s", userId, entity.id)
        return NewAuthSession(entity.toModel(), "${entity.id}|$secret")
    }

    override fun sessions(userId: Long): List<AuthSession> =
        repository.listForUser(userId).map(AuthSessionEntity::toModel)

    @Transactional
    override fun revoke(userId: Long, sessionId: UUID) {
        val session = repository.findForUser(userId, sessionId)
            ?: throw ApiException(ErrorCode.SESSION_NOT_FOUND)
        if (session.revokedAt == null) session.revokedAt = Instant.now()
        log.infof("auth event=session_revoked user_id=%d session_id=%s", userId, sessionId)
    }

    @Transactional
    override fun revokeOthers(userId: Long, exceptSessionId: UUID) {
        repository.update(
            "revokedAt = ?1 where userId = ?2 and id <> ?3 and revokedAt is null",
            Instant.now(),
            userId,
            exceptSessionId,
        )
    }
}
