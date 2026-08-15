package com.example.starter.internal.auth.cleanup

import com.example.starter.internal.auth.config.AuthConfig
import com.example.starter.internal.auth.session.AuthSessionRepository
import com.example.starter.internal.auth.token.PersonalAccessTokenRepository
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.time.Instant

@ApplicationScoped
class CredentialCleanupJob(
    private val tokenRepository: PersonalAccessTokenRepository,
    private val sessionRepository: AuthSessionRepository,
    private val config: AuthConfig,
) {
    private val log = Logger.getLogger(CredentialCleanupJob::class.java)

    @Scheduled(every = "1h", identity = "auth-credential-cleanup")
    @Transactional
    fun cleanup() {
        val now = Instant.now()
        val revokedBefore = now.minus(config.revokedRetention())
        val deletedTokens = tokenRepository.deleteExpiredOrOldRevoked(now, revokedBefore)
        val deletedSessions = sessionRepository.deleteExpiredOrOldRevoked(now, revokedBefore)
        if (deletedTokens + deletedSessions > 0) {
            log.infof(
                "auth event=credentials_cleaned tokens=%d sessions=%d",
                deletedTokens,
                deletedSessions,
            )
        }
    }
}
