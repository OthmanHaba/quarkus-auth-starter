package com.example.starter.application.auth

import com.example.starter.application.auth.model.NewAccessToken
import com.example.starter.application.auth.model.PersonalAccessToken
import java.time.Instant

interface PersonalTokens {
    fun createToken(
        userId: Long,
        name: String,
        abilities: Set<String> = setOf("*"),
        expiresAt: Instant? = null,
    ): NewAccessToken

    fun tokens(userId: Long): List<PersonalAccessToken>
    fun findToken(userId: Long, tokenId: Long): PersonalAccessToken?
    fun revokeToken(userId: Long, tokenId: Long)
    fun revokeAllTokens(userId: Long)
    fun revokeOtherTokens(userId: Long, exceptTokenId: Long)
}
