package com.example.starter.http.auth

import com.example.starter.application.auth.CurrentAuth
import com.example.starter.application.auth.SessionManager
import com.example.starter.application.auth.model.AuthSession
import com.example.starter.application.authorization.TokenAbilities
import com.example.starter.shared.ApiException
import com.example.starter.shared.ErrorCode
import io.quarkus.security.PermissionsAllowed
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

@Path("/api/auth/sessions")
@Produces(MediaType.APPLICATION_JSON)
@TokenAbilities("sessions:read")
class SessionResource(
    private val currentAuth: CurrentAuth,
    private val sessionManager: SessionManager,
) {
    @GET
    @PermissionsAllowed("sessions:read")
    fun list(): List<AuthSession> = sessionManager.sessions(currentAuth.userId())

    @DELETE
    @Path("/{sessionId}")
    @TokenAbilities("sessions:revoke")
    fun revoke(@PathParam("sessionId") sessionId: UUID): Response {
        sessionManager.revoke(currentAuth.userId(), sessionId)
        return Response.noContent().build()
    }

    @DELETE
    @Path("/others")
    @TokenAbilities("sessions:revoke")
    fun revokeOthers(): Response {
        val session = currentAuth.currentSession() ?: throw ApiException(ErrorCode.SESSION_NOT_FOUND)
        sessionManager.revokeOthers(currentAuth.userId(), session.id)
        return Response.noContent().build()
    }
}
