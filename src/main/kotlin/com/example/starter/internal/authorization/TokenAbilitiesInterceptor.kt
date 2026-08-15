package com.example.starter.internal.authorization

import com.example.starter.application.authorization.TokenAbilities
import io.quarkus.arc.ArcInvocationContext
import jakarta.annotation.Priority
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InvocationContext

@TokenAbilities
@Interceptor
@Priority(Interceptor.Priority.APPLICATION + 10)
class TokenAbilitiesInterceptor(
    private val authorizer: TokenAbilityAuthorizer,
) {
    @AroundInvoke
    fun authorize(context: InvocationContext): Any? {
        val binding = (context as ArcInvocationContext).interceptorBindings
            .filterIsInstance<TokenAbilities>()
            .first()
        authorizer.requireAll(binding.value)
        return context.proceed()
    }
}
