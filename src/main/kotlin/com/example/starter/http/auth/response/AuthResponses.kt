package com.example.starter.http.auth.response

import com.example.starter.application.auth.AuthenticationType
import com.example.starter.application.auth.model.AuthSession
import com.example.starter.application.auth.model.PersonalAccessToken
import com.example.starter.application.auth.model.AuthenticatedUser
import com.example.starter.application.action.auth.AuthenticatedUserResult
import com.example.starter.domain.user.UserEntity
import com.example.starter.domain.user.UserRole
import java.time.Instant

data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    val role: UserRole,
    val createdAt: Instant,
)

data class SessionLoginResponse(
    val user: UserDto,
    val session: AuthSession,
)

data class CurrentAuthResponse(
    val user: AuthenticatedUser,
    val type: AuthenticationType,
    val token: PersonalAccessToken?,
    val session: AuthSession?,
)

fun UserEntity.toDto() = UserDto(
    id = requireNotNull(id),
    name = name,
    email = email,
    role = role,
    createdAt = createdAt,
)

fun AuthenticatedUserResult.toDto() = UserDto(id, name, email, role, createdAt)
