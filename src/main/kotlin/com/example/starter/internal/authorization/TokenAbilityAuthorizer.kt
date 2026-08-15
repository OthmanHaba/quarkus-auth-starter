package com.example.starter.internal.authorization

import com.example.starter.application.auth.CurrentAuth
import com.example.starter.shared.ApiException
import com.example.starter.shared.ErrorCode
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class TokenAbilityAuthorizer(private val currentAuth: CurrentAuth) {
    fun requireAll(abilities: Array<out String>) {
        currentAuth.user()
        if (!currentAuth.tokenCanAll(*abilities)) throw ApiException(ErrorCode.FORBIDDEN)
    }

    fun requireAny(abilities: Array<out String>) {
        currentAuth.user()
        if (!currentAuth.tokenCanAny(*abilities)) throw ApiException(ErrorCode.FORBIDDEN)
    }
}
