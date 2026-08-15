package com.example.starter.http.user

import com.example.starter.application.authorization.Ability
import com.example.starter.application.authorization.Gate
import com.example.starter.domain.user.UserEntity
import com.example.starter.domain.user.UserRepository
import com.example.starter.http.auth.response.UserDto
import com.example.starter.http.auth.response.toDto
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
class UserResource(
    private val userRepository: UserRepository,
) {
    @GET
    @Gate(Ability.MANAGE_USERS)
    fun list(): List<UserDto> {
        return userRepository.listByName().map(UserEntity::toDto)
    }
}
