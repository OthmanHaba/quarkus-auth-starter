package com.example.starter.internal.auth.security

import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.IdentityProvider
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class SessionIdentityProvider(
    private val validator: CredentialValidator,
    private val identityFactory: SecurityIdentityFactory,
) : IdentityProvider<SessionAuthenticationRequest> {
    override fun getRequestType() = SessionAuthenticationRequest::class.java

    override fun authenticate(
        request: SessionAuthenticationRequest,
        context: AuthenticationRequestContext,
    ): Uni<SecurityIdentity> = context.runBlocking {
        val validated = validator.validateSession(request.plainTextSession)
        identityFactory.forSession(validated.user, validated.session)
    }
}
