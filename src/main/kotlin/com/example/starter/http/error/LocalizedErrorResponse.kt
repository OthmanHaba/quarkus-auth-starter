package com.example.starter.http.error

import com.example.starter.shared.ApiErrorDetail
import com.example.starter.shared.ApiErrorResponse
import com.example.starter.shared.ErrorCode
import com.example.starter.http.localization.Localizer
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Response

fun localizedErrorResponse(
    localizer: Localizer,
    code: ErrorCode,
    messageKey: String = code.messageKey,
    messageArguments: List<Any> = emptyList(),
    details: List<ApiErrorDetail> = emptyList(),
): Response = Response.status(code.status)
    .header(HttpHeaders.CONTENT_LANGUAGE, localizer.languageTag)
    .entity(
        ApiErrorResponse(
            code = code,
            message = localizer.message(messageKey, *messageArguments.toTypedArray()),
            details = details,
        ),
    )
    .build()
