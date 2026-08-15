package com.example.starter.http.filter

import com.example.starter.application.auth.AuthenticationType
import com.example.starter.application.auth.CurrentAuth
import com.example.starter.internal.auth.config.AuthConfig
import com.example.starter.internal.auth.security.CredentialSecrets
import com.example.starter.shared.ApiException
import com.example.starter.shared.ErrorCode
import io.vertx.ext.web.RoutingContext
import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.ext.Provider

@Provider
@Priority(Priorities.AUTHORIZATION + 10)
class CsrfProtectionFilter(
    private val currentAuth: CurrentAuth,
    private val config: AuthConfig,
    private val routingContext: RoutingContext,
) : ContainerRequestFilter {
    override fun filter(requestContext: ContainerRequestContext) {
        if (requestContext.method !in STATE_CHANGING_METHODS) return
        if (currentAuth.type() != AuthenticationType.SESSION) return

        val cookieValue = routingContext.request().getCookie(config.csrfCookieName())?.value
            ?: throw ApiException(ErrorCode.CSRF_TOKEN_MISMATCH)
        val headerValue = requestContext.getHeaderString("X-XSRF-TOKEN")
            ?: throw ApiException(ErrorCode.CSRF_TOKEN_MISMATCH)
        if (!CredentialSecrets.matches(headerValue, CredentialSecrets.hash(cookieValue))) {
            throw ApiException(ErrorCode.CSRF_TOKEN_MISMATCH)
        }
    }

    private companion object {
        val STATE_CHANGING_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
    }
}
