package com.example.starter.application.auth

import com.example.starter.application.auth.model.AuthSession
import com.example.starter.application.auth.model.PersonalAccessToken
import com.example.starter.application.auth.model.AuthenticatedUser

interface CurrentAuth {
    fun check(): Boolean
    fun guest(): Boolean
    fun user(): AuthenticatedUser
    fun userId(): Long
    fun type(): AuthenticationType
    fun currentToken(): PersonalAccessToken?
    fun currentSession(): AuthSession?
    fun tokenCan(ability: String): Boolean
    fun tokenCannot(ability: String): Boolean
    fun tokenCanAny(vararg abilities: String): Boolean
    fun tokenCanAll(vararg abilities: String): Boolean
}
