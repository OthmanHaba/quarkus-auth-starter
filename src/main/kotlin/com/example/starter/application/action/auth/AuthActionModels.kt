package com.example.starter.application.action.auth

import com.example.starter.application.auth.model.NewAuthSession
import com.example.starter.domain.user.UserRole
import java.time.Instant

data class LoginCommand(val email: String, val password: String)

data class RegisterCommand(val name: String, val email: String, val password: String)

data class AuthenticatedUserResult(
    val id: Long,
    val name: String,
    val email: String,
    val role: UserRole,
    val createdAt: Instant,
)

data class LoginResult(
    val user: AuthenticatedUserResult,
    val newSession: NewAuthSession,
)
