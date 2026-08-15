package com.example.starter.internal.auth.token

import com.example.starter.application.auth.PersonalTokens
import com.example.starter.application.auth.model.NewAccessToken
import com.example.starter.application.auth.model.PersonalAccessToken
import com.example.starter.internal.auth.config.AuthConfig
import com.example.starter.internal.auth.security.CredentialSecrets
import com.example.starter.shared.ApiException
import com.example.starter.shared.ErrorCode
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.time.Instant

@ApplicationScoped
class DatabasePersonalTokens(
    private val repository: PersonalAccessTokenRepository,
    private val config: AuthConfig,
) : PersonalTokens {
    private val log = Logger.getLogger(DatabasePersonalTokens::class.java)

    @Transactional
    override fun createToken(
        userId: Long,
        name: String,
        abilities: Set<String>,
        expiresAt: Instant?,
    ): NewAccessToken {
        val normalizedName = validateName(name)
        val normalizedAbilities = normalizeAbilities(abilities)
        val secret = CredentialSecrets.generate()
        val entity = PersonalAccessTokenEntity().apply {
            this.userId = userId
            this.name = normalizedName
            tokenHash = CredentialSecrets.hash(secret)
            this.abilities = normalizedAbilities.toMutableSet()
            this.expiresAt = expiresAt ?: Instant.now().plus(config.tokenDefaultExpiration())
        }
        repository.persist(entity)
        repository.flush()
        val token = entity.toModel()
        log.infof("auth event=personal_token_created user_id=%d token_id=%d", userId, token.id)
        return NewAccessToken(token, "${token.id}|$secret")
    }

    override fun tokens(userId: Long): List<PersonalAccessToken> =
        repository.listForUser(userId).map(PersonalAccessTokenEntity::toModel)

    override fun findToken(userId: Long, tokenId: Long): PersonalAccessToken? =
        repository.findForUser(userId, tokenId)?.toModel()

    @Transactional
    override fun revokeToken(userId: Long, tokenId: Long) {
        val token = repository.findForUser(userId, tokenId)
            ?: throw ApiException(ErrorCode.TOKEN_NOT_FOUND)
        if (token.revokedAt == null) token.revokedAt = Instant.now()
        log.infof("auth event=personal_token_revoked user_id=%d token_id=%d", userId, tokenId)
    }

    @Transactional
    override fun revokeAllTokens(userId: Long) {
        repository.update("revokedAt = ?1 where userId = ?2 and revokedAt is null", Instant.now(), userId)
        log.infof("auth event=all_tokens_revoked user_id=%d", userId)
    }

    @Transactional
    override fun revokeOtherTokens(userId: Long, exceptTokenId: Long) {
        repository.update(
            "revokedAt = ?1 where userId = ?2 and id <> ?3 and revokedAt is null",
            Instant.now(),
            userId,
            exceptTokenId,
        )
    }

    private fun validateName(name: String): String {
        val normalized = name.trim()
        if (normalized.isEmpty() || normalized.length > 255) {
            throw ApiException(ErrorCode.VALIDATION_ERROR, "api.error.invalid_token_name")
        }
        return normalized
    }

    private fun normalizeAbilities(abilities: Set<String>): Set<String> {
        val normalized = abilities.map(String::trim).toSet()
        if (normalized.isEmpty() || normalized.size > 50 || normalized.any { it.isEmpty() || it.length > 100 }) {
            throw ApiException(ErrorCode.VALIDATION_ERROR, "api.error.invalid_token_abilities")
        }
        return normalized
    }
}
