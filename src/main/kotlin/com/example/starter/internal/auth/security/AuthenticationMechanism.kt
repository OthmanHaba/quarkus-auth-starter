package com.example.starter.internal.auth.security

import com.example.starter.internal.auth.config.AuthConfig
import com.example.starter.internal.auth.config.StatefulOriginMatcher
import io.quarkus.security.identity.IdentityProviderManager
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.request.AuthenticationRequest
import io.quarkus.vertx.http.runtime.security.ChallengeData
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils
import io.smallrye.mutiny.Uni
import io.vertx.core.http.HttpHeaders
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class AuthenticationMechanism(
    private val config: AuthConfig,
    private val originMatcher: StatefulOriginMatcher,
) : HttpAuthenticationMechanism {
    override fun authenticate(
        context: RoutingContext,
        identityProviderManager: IdentityProviderManager,
    ): Uni<SecurityIdentity> {
        val sessionCookie = if (originMatcher.isTrusted(context)) {
            context.request().getCookie(config.sessionCookieName())?.value
        } else {
            null
        }

        if (sessionCookie != null) {
            val request = SessionAuthenticationRequest(sessionCookie)
            HttpSecurityUtils.setRoutingContextAttribute(request, context)
            return identityProviderManager.authenticate(request)
        }

        val bearerToken = bearerToken(context) ?: return Uni.createFrom().nullItem()
        val request = PersonalTokenAuthenticationRequest(bearerToken)
        HttpSecurityUtils.setRoutingContextAttribute(request, context)
        return identityProviderManager.authenticate(request)
    }

    override fun getChallenge(context: RoutingContext): Uni<ChallengeData> =
        Uni.createFrom().item(ChallengeData(401, "WWW-Authenticate", "Bearer"))

    override fun getCredentialTypes(): Set<Class<out AuthenticationRequest>> = setOf(
        SessionAuthenticationRequest::class.java,
        PersonalTokenAuthenticationRequest::class.java,
    )

    private fun bearerToken(context: RoutingContext): String? {
        val header = context.request().getHeader(HttpHeaders.AUTHORIZATION) ?: return null
        if (!header.regionMatches(0, "Bearer ", 0, 7, ignoreCase = true)) return null
        return header.substring(7).trim().takeIf(String::isNotEmpty)
    }
}
