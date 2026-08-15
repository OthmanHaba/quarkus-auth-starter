package com.example.starter.application.action.auth

import com.example.starter.application.auth.SessionManager
import com.example.starter.domain.user.UserRepository
import com.example.starter.shared.ApiException
import com.example.starter.shared.ErrorCode
import io.quarkus.elytron.security.common.BcryptUtil
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger

@ApplicationScoped
class LoginAction(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
) {
    private val log = Logger.getLogger(LoginAction::class.java)

    fun execute(command: LoginCommand, ipAddress: String?, userAgent: String?): LoginResult {
        val user = userRepository.findByEmail(command.email.trim().lowercase())
        if (user == null || !user.active || !BcryptUtil.matches(command.password, user.passwordHash)) {
            log.warnf("auth event=login_failed category=invalid_credentials")
            throw ApiException(ErrorCode.INVALID_CREDENTIALS)
        }

        val userId = requireNotNull(user.id)
        val session = sessionManager.create(userId, ipAddress, userAgent)
        log.infof("auth event=login_succeeded user_id=%d session_id=%s", userId, session.session.id)
        return LoginResult(
            user = AuthenticatedUserResult(
                id = userId,
                name = user.name,
                email = user.email,
                role = user.role,
                createdAt = user.createdAt,
            ),
            newSession = session,
        )
    }
}
