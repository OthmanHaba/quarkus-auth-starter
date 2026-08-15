package com.example.starter.http.auth.request

import com.example.starter.application.action.auth.LoginCommand
import com.example.starter.application.action.auth.RegisterCommand
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "validation.auth.name.required")
    @field:Size(max = 255, message = "validation.auth.name.max")
    val name: String = "",

    @field:NotBlank(message = "validation.auth.email.required")
    @field:Email(message = "validation.auth.email.invalid")
    @field:Size(max = 320, message = "validation.auth.email.max")
    val email: String = "",

    @field:Size(min = 8, max = 72, message = "validation.auth.password.size")
    val password: String = "",
)

data class LoginRequest(
    @field:NotBlank(message = "validation.auth.email.required")
    @field:Email(message = "validation.auth.email.invalid")
    val email: String = "",

    @field:NotBlank(message = "validation.auth.password.required")
    val password: String = "",
)

fun RegisterRequest.toCommand() = RegisterCommand(name, email, password)

fun LoginRequest.toCommand() = LoginCommand(email, password)
