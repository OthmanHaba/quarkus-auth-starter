package com.example.starter.http.error

import com.example.starter.shared.ErrorCode
import com.example.starter.http.localization.Localizer
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class MalformedRequestExceptionMapper(private val localizer: Localizer) : ExceptionMapper<WebApplicationException> {
    override fun toResponse(exception: WebApplicationException): Response {
        if (exception.response.status != Response.Status.BAD_REQUEST.statusCode) {
            return exception.response
        }

        return localizedErrorResponse(localizer, ErrorCode.MALFORMED_REQUEST)
    }
}
