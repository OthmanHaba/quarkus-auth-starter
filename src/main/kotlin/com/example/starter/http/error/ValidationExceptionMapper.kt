package com.example.starter.http.error

import com.example.starter.shared.ApiErrorDetail
import com.example.starter.shared.ErrorCode
import com.example.starter.http.localization.Localizer
import jakarta.validation.ConstraintViolationException
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class ValidationExceptionMapper(private val localizer: Localizer) : ExceptionMapper<ConstraintViolationException> {
    override fun toResponse(exception: ConstraintViolationException) =
        localizedErrorResponse(
            localizer = localizer,
            code = ErrorCode.VALIDATION_ERROR,
            details = validationDetails(exception),
        )

    private fun validationDetails(exception: ConstraintViolationException): List<ApiErrorDetail> =
        exception.constraintViolations
            .map { violation ->
                ApiErrorDetail(
                    field = violation.propertyPath.toString().substringAfterLast('.'),
                    message = localizer.message(violation.message),
                )
            }
            .sortedWith(compareBy(ApiErrorDetail::field, ApiErrorDetail::message))
}
