package com.example.starter.application.auth.model

data class AuthenticatedUser(
    val id: Long,
    val principalName: String,
    val roles: Set<String>,
)
