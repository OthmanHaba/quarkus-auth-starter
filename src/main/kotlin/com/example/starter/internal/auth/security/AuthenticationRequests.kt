package com.example.starter.internal.auth.security

import io.quarkus.security.identity.request.BaseAuthenticationRequest

class SessionAuthenticationRequest(
    val plainTextSession: String,
) : BaseAuthenticationRequest()

class PersonalTokenAuthenticationRequest(
    val plainTextToken: String,
) : BaseAuthenticationRequest()
