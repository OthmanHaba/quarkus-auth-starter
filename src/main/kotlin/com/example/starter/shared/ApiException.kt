package com.example.starter.shared

class ApiException(
    val code: ErrorCode,
    val messageKey: String = code.messageKey,
    val messageArguments: List<Any> = emptyList(),
    val details: List<ApiErrorDetail> = emptyList(),
) : RuntimeException(code.name)
