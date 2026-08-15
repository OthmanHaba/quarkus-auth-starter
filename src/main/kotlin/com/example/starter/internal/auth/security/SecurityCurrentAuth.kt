package com.example.starter.internal.auth.security

import com.example.starter.application.auth.AuthenticationType
import com.example.starter.application.auth.CurrentAuth
import com.example.starter.application.auth.model.AuthSession
import com.example.starter.application.auth.model.PersonalAccessToken
import com.example.starter.application.auth.model.AuthenticatedUser
import com.example.starter.shared.ApiException
import com.example.starter.shared.ErrorCode
import io.quarkus.security.identity.SecurityIdentity
import jakarta.enterprise.context.RequestScoped

@RequestScoped
class SecurityCurrentAuth(
    private val identity: SecurityIdentity,
) : CurrentAuth {
    override fun check(): Boolean = !identity.isAnonymous && authenticatedUser() != null

    override fun guest(): Boolean = !check()

    override fun user(): AuthenticatedUser = authenticatedUser()
        ?: throw ApiException(ErrorCode.AUTHENTICATION_REQUIRED)

    override fun userId(): Long = user().id

    override fun type(): AuthenticationType =
        identity.getAttribute(AuthIdentityAttributes.AUTH_TYPE) ?: AuthenticationType.ANONYMOUS

    override fun currentToken(): PersonalAccessToken? =
        identity.getAttribute(AuthIdentityAttributes.TOKEN)

    override fun currentSession(): AuthSession? =
        identity.getAttribute(AuthIdentityAttributes.SESSION)

    override fun tokenCan(ability: String): Boolean = when (type()) {
        AuthenticationType.SESSION -> true
        AuthenticationType.PERSONAL_ACCESS_TOKEN -> currentToken()?.can(ability) == true
        AuthenticationType.ANONYMOUS -> false
    }

    override fun tokenCannot(ability: String): Boolean = !tokenCan(ability)

    override fun tokenCanAny(vararg abilities: String): Boolean = abilities.any(::tokenCan)

    override fun tokenCanAll(vararg abilities: String): Boolean = abilities.all(::tokenCan)

    private fun authenticatedUser(): AuthenticatedUser? =
        identity.getAttribute(AuthIdentityAttributes.USER)
}
