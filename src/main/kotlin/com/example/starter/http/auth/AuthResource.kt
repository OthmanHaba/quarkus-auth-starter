package com.example.starter.http.auth

import com.example.starter.application.action.auth.LoginAction
import com.example.starter.application.action.auth.RegisterAction
import com.example.starter.application.auth.AuthenticationType
import com.example.starter.application.auth.CurrentAuth
import com.example.starter.application.auth.PersonalTokens
import com.example.starter.application.auth.SessionManager
import com.example.starter.internal.auth.config.StatefulOriginMatcher
import com.example.starter.internal.auth.cookie.AuthCookieManager
import com.example.starter.internal.auth.security.CredentialSecrets
import com.example.starter.shared.ApiException
import com.example.starter.shared.ErrorCode
import com.example.starter.http.auth.request.LoginRequest
import com.example.starter.http.auth.request.RegisterRequest
import com.example.starter.http.auth.request.toCommand
import com.example.starter.http.auth.response.CurrentAuthResponse
import com.example.starter.http.auth.response.SessionLoginResponse
import com.example.starter.http.auth.response.toDto
import io.vertx.ext.web.RoutingContext
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class AuthResource(
    private val registerAction: RegisterAction,
    private val loginAction: LoginAction,
    private val currentAuth: CurrentAuth,
    private val sessionManager: SessionManager,
    private val personalTokens: PersonalTokens,
    private val originMatcher: StatefulOriginMatcher,
    private val cookies: AuthCookieManager,
    private val context: RoutingContext,
) {
    @POST
    @Path("/register")
    fun register(@Valid request: RegisterRequest): Response =
        Response.status(Response.Status.CREATED).entity(registerAction.execute(request.toCommand()).toDto()).build()

    @POST
    @Path("/login")
    fun login(@Valid request: LoginRequest): SessionLoginResponse {

        if (
            !originMatcher.isTrusted(context)
        ) throw ApiException(ErrorCode.UNTRUSTED_ORIGIN)

        currentAuth.currentSession()?.let { sessionManager.revoke(currentAuth.userId(), it.id) }

        val result = loginAction.execute(
            request.toCommand(),
            context.request().remoteAddress()?.host(),
            context.request().getHeader("User-Agent"),
        )
        cookies.setSession(context, result.newSession.plainTextSession)
        return SessionLoginResponse(result.user.toDto(), result.newSession.session)
    }

    @POST
    @Path("/logout")
    fun logout(): Response {
        val userId = currentAuth.userId()
        when (currentAuth.type()) {
            AuthenticationType.SESSION -> {
                sessionManager.revoke(userId, requireNotNull(currentAuth.currentSession()).id)
                cookies.clearSession(context)
            }
            AuthenticationType.PERSONAL_ACCESS_TOKEN ->
                personalTokens.revokeToken(userId, requireNotNull(currentAuth.currentToken()).id)
            AuthenticationType.ANONYMOUS -> throw ApiException(ErrorCode.AUTHENTICATION_REQUIRED)
        }
        return Response.noContent().build()
    }

    @GET
    @Path("/me")
    fun me() = CurrentAuthResponse(
        user = currentAuth.user(),
        type = currentAuth.type(),
        token = currentAuth.currentToken(),
        session = currentAuth.currentSession(),
    )

    @GET
    @Path("/csrf-cookie")
    fun csrfCookie(): Response {
        if (!originMatcher.isTrusted(context)) throw ApiException(ErrorCode.UNTRUSTED_ORIGIN)
        cookies.setCsrf(context, CredentialSecrets.generate())
        return Response.noContent().build()
    }
}
