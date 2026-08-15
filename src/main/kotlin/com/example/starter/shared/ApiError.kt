package com.example.starter.shared

import java.time.Instant

data class ApiErrorDetail(
    val field: String,
    val message: String,
)

data class ApiErrorResponse(
    val code: ErrorCode,
    val message: String,
    val details: List<ApiErrorDetail> = emptyList(),
    val timestamp: Instant = Instant.now(),
)
