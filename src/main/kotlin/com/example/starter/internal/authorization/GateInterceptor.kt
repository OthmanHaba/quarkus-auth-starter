package com.example.starter.internal.authorization

import com.example.starter.application.auth.CurrentAuth
import com.example.starter.application.auth.model.AuthenticatedUser
import com.example.starter.application.authorization.Ability
import com.example.starter.application.authorization.Gate
import com.example.starter.shared.ApiException
import com.example.starter.shared.ErrorCode
import io.quarkus.arc.ArcInvocationContext
import jakarta.annotation.Priority
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InvocationContext

@Gate(Ability.MANAGE_USERS)
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
class GateInterceptor(private val currentAuth: CurrentAuth) {
    @AroundInvoke
    fun authorize(context: InvocationContext): Any? {
        val gate = (context as ArcInvocationContext).interceptorBindings
            .filterIsInstance<Gate>()
            .first()
        val user = currentAuth.user()

        if (!permits(user, gate.value)) {
            throw ApiException(code = ErrorCode.FORBIDDEN)
        }

        return context.proceed()
    }

    private fun permits(user: AuthenticatedUser, ability: Ability): Boolean = when (ability) {
        Ability.MANAGE_USERS -> "ADMIN" in user.roles
    }
}
