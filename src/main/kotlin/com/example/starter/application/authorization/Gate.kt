package com.example.starter.application.authorization

import jakarta.enterprise.util.Nonbinding
import jakarta.interceptor.InterceptorBinding

@MustBeDocumented
@InterceptorBinding
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Gate(
    @get:Nonbinding val value: Ability,
)
