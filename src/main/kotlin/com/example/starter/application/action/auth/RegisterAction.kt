package com.example.starter.application.action.auth

import com.example.starter.domain.user.UserEntity
import com.example.starter.domain.user.UserRepository
import com.example.starter.shared.ApiException
import com.example.starter.shared.ErrorCode
import io.quarkus.elytron.security.common.BcryptUtil
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class RegisterAction(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun execute(command: RegisterCommand): AuthenticatedUserResult {
        val email = command.email.trim().lowercase()
        if (userRepository.findByEmail(email) != null) {
            throw ApiException(
                code = ErrorCode.EMAIL_ALREADY_REGISTERED,
            )
        }

        val user = UserEntity().apply {
            name = command.name.trim()
            this.email = email
            passwordHash = BcryptUtil.bcryptHash(command.password)
        }
        userRepository.persist(user)
        userRepository.flush()

        return AuthenticatedUserResult(
            id = requireNotNull(user.id),
            name = user.name,
            email = user.email,
            role = user.role,
            createdAt = user.createdAt,
        )
    }
}
