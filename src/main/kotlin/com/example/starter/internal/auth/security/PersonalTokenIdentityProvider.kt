package com.example.starter.internal.auth.security

import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.IdentityProvider
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class PersonalTokenIdentityProvider(
    private val validator: CredentialValidator,
    private val identityFactory: SecurityIdentityFactory,
) : IdentityProvider<PersonalTokenAuthenticationRequest> {
    override fun getRequestType() = PersonalTokenAuthenticationRequest::class.java

    override fun authenticate(
        request: PersonalTokenAuthenticationRequest,
        context: AuthenticationRequestContext,
    ): Uni<SecurityIdentity> = context.runBlocking {
        val validated = validator.validateToken(request.plainTextToken)
        identityFactory.forToken(validated.user, validated.token)
    }
}
