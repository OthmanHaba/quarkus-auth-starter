package com.example.starter.internal.authorization

import com.example.starter.application.authorization.TokenAbilityAny
import io.quarkus.arc.ArcInvocationContext
import jakarta.annotation.Priority
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InvocationContext

@TokenAbilityAny
@Interceptor
@Priority(Interceptor.Priority.APPLICATION + 10)
class TokenAbilityAnyInterceptor(
    private val authorizer: TokenAbilityAuthorizer,
) {
    @AroundInvoke
    fun authorize(context: InvocationContext): Any? {
        val binding = (context as ArcInvocationContext).interceptorBindings
            .filterIsInstance<TokenAbilityAny>()
            .first()
        authorizer.requireAny(binding.value)
        return context.proceed()
    }
}
