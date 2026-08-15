package com.example.starter.internal.auth.security

import com.example.starter.internal.auth.config.AuthConfig
import com.example.starter.application.auth.model.AuthSession
import com.example.starter.internal.auth.session.AuthSessionRepository
import com.example.starter.internal.auth.session.ParsedSessionToken
import com.example.starter.internal.auth.token.ParsedPersonalToken
import com.example.starter.application.auth.model.PersonalAccessToken
import com.example.starter.internal.auth.token.PersonalAccessTokenRepository
import com.example.starter.application.auth.model.AuthenticatedUser
import com.example.starter.internal.auth.user.AuthUserProvider
import io.quarkus.security.AuthenticationFailedException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.control.ActivateRequestContext
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.time.Instant

@ApplicationScoped
class CredentialValidator(
    private val tokenRepository: PersonalAccessTokenRepository,
    private val sessionRepository: AuthSessionRepository,
    private val userProvider: AuthUserProvider,
    private val config: AuthConfig,
) {
    private val log = Logger.getLogger(CredentialValidator::class.java)

    @Transactional
    @ActivateRequestContext
    fun validateToken(plainTextToken: String): ValidatedToken {
        val parsed = ParsedPersonalToken.parse(plainTextToken) ?: fail("malformed_personal_token")
        val entity = tokenRepository.findById(parsed.id) ?: fail("unknown_personal_token", parsed.id)
        if (!CredentialSecrets.matches(parsed.secret, entity.tokenHash)) fail("incorrect_personal_token", parsed.id)
        val now = Instant.now()
        if (entity.revokedAt != null) fail("revoked_personal_token", parsed.id)
        if (entity.expiresAt?.isAfter(now) == false) fail("expired_personal_token", parsed.id)
        val user = userProvider.findById(entity.userId) ?: fail("inactive_token_owner", parsed.id)
        if (shouldUpdate(entity.lastUsedAt, now)) entity.lastUsedAt = now
        return ValidatedToken(user, entity.toModel())
    }

    @Transactional
    @ActivateRequestContext
    fun validateSession(plainTextSession: String): ValidatedSession {
        val parsed = ParsedSessionToken.parse(plainTextSession) ?: fail("malformed_session")
        val entity = sessionRepository.findById(parsed.id) ?: fail("unknown_session", sessionId = parsed.id.toString())
        if (!CredentialSecrets.matches(parsed.secret, entity.sessionHash)) {
            fail("incorrect_session", sessionId = parsed.id.toString())
        }
        val now = Instant.now()
        if (entity.revokedAt != null) fail("revoked_session", sessionId = parsed.id.toString())
        if (!entity.expiresAt.isAfter(now)) fail("expired_session", sessionId = parsed.id.toString())
        val user = userProvider.findById(entity.userId) ?: fail("inactive_session_owner", sessionId = parsed.id.toString())
        if (shouldUpdate(entity.lastUsedAt, now)) entity.lastUsedAt = now
        return ValidatedSession(user, entity.toModel())
    }

    private fun shouldUpdate(lastUsedAt: Instant?, now: Instant): Boolean =
        lastUsedAt == null || lastUsedAt.plus(config.lastUsedUpdateInterval()).isBefore(now)

    private fun fail(category: String, tokenId: Long? = null, sessionId: String? = null): Nothing {
        log.warnf(
            "auth event=authentication_failed category=%s token_id=%s session_id=%s",
            category,
            tokenId?.toString() ?: "-",
            sessionId ?: "-",
        )
        throw AuthenticationFailedException()
    }
}

data class ValidatedToken(val user: AuthenticatedUser, val token: PersonalAccessToken)
data class ValidatedSession(val user: AuthenticatedUser, val session: AuthSession)
