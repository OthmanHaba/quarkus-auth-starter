package com.example.starter.internal.auth.user

import com.example.starter.application.auth.model.AuthenticatedUser
import com.example.starter.domain.user.UserRepository
import jakarta.enterprise.context.ApplicationScoped

interface AuthUserProvider {
    fun findById(id: Long): AuthenticatedUser?
}

@ApplicationScoped
class ApplicationAuthUserProvider(
    private val userRepository: UserRepository,
) : AuthUserProvider {
    override fun findById(id: Long): AuthenticatedUser? =
        userRepository.findActiveById(id)?.let { user ->
            AuthenticatedUser(
                id = requireNotNull(user.id),
                principalName = user.email,
                roles = setOf(user.role.name),
            )
        }
}
