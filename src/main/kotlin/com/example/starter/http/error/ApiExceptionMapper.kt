package com.example.starter.http.error

import com.example.starter.shared.ApiException
import com.example.starter.http.localization.Localizer
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class ApiExceptionMapper(private val localizer: Localizer) : ExceptionMapper<ApiException> {
    override fun toResponse(exception: ApiException) = localizedErrorResponse(
        localizer = localizer,
        code = exception.code,
        messageKey = exception.messageKey,
        messageArguments = exception.messageArguments,
        details = exception.details,
    )
}
