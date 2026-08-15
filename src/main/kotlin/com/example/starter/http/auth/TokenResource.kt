package com.example.starter.http.auth

import com.example.starter.application.auth.CurrentAuth
import com.example.starter.application.auth.PersonalTokens
import com.example.starter.application.auth.model.NewAccessToken
import com.example.starter.application.auth.model.PersonalAccessToken
import com.example.starter.application.authorization.TokenAbilities
import com.example.starter.application.authorization.TokenAbilityAny
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.time.Instant

data class CreateTokenRequest(
    @field:NotBlank(message = "validation.token_name.required")
    @field:Size(max = 255, message = "validation.token_name.max")
    val name: String = "",
    val abilities: Set<String> = setOf("*"),
    val expiresAt: Instant? = null,
)

@Path("/api/auth/tokens")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class TokenResource(
    private val currentAuth: CurrentAuth,
    private val personalTokens: PersonalTokens,
) {
    @GET
    @TokenAbilityAny("tokens:read", "tokens:manage")
    fun list(): List<PersonalAccessToken> = personalTokens.tokens(currentAuth.userId())

    @POST
    @TokenAbilities("tokens:create")
    fun create(@Valid request: CreateTokenRequest): Response {
        val token: NewAccessToken = personalTokens.createToken(
            userId = currentAuth.userId(),
            name = request.name,
            abilities = request.abilities,
            expiresAt = request.expiresAt,
        )
        return Response.status(Response.Status.CREATED).entity(token).build()
    }

    @DELETE
    @Path("/{tokenId}")
    @TokenAbilities("tokens:revoke")
    fun revoke(@PathParam("tokenId") tokenId: Long): Response {
        personalTokens.revokeToken(currentAuth.userId(), tokenId)
        return Response.noContent().build()
    }
}
