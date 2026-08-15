package com.example.starter.internal.auth.security

import com.example.starter.application.auth.AuthenticationType
import com.example.starter.application.auth.model.AuthSession
import com.example.starter.application.auth.model.PersonalAccessToken
import com.example.starter.application.auth.model.AuthenticatedUser
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.runtime.QuarkusPrincipal
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class SecurityIdentityFactory {
    fun forSession(user: AuthenticatedUser, session: AuthSession): SecurityIdentity = base(user)
        .addAttribute(AuthIdentityAttributes.AUTH_TYPE, AuthenticationType.SESSION)
        .addAttribute(AuthIdentityAttributes.SESSION, session)
        .addAttribute(AuthIdentityAttributes.SESSION_ID, session.id)
        .addPermissionChecker { Uni.createFrom().item(true) }
        .build()

    fun forToken(user: AuthenticatedUser, token: PersonalAccessToken): SecurityIdentity = base(user)
        .addAttribute(AuthIdentityAttributes.AUTH_TYPE, AuthenticationType.PERSONAL_ACCESS_TOKEN)
        .addAttribute(AuthIdentityAttributes.TOKEN, token)
        .addAttribute(AuthIdentityAttributes.TOKEN_ID, token.id)
        .addPermissionsAsString(token.abilities.filterNot { it == "*" }.toSet())
        .addPermissionChecker { permission -> Uni.createFrom().item(token.can(permission.name)) }
        .build()

    private fun base(user: AuthenticatedUser) = QuarkusSecurityIdentity.builder()
        .setPrincipal(QuarkusPrincipal(user.principalName))
        .addRoles(user.roles)
        .addAttribute(AuthIdentityAttributes.USER, user)
}
