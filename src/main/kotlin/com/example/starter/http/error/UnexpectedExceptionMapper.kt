package com.example.starter.http.error

import com.example.starter.shared.ErrorCode
import com.example.starter.http.localization.Localizer
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.jboss.logging.Logger

@Provider
class UnexpectedExceptionMapper(private val localizer: Localizer) : ExceptionMapper<Throwable> {
    private val logger = Logger.getLogger(UnexpectedExceptionMapper::class.java)

    override fun toResponse(exception: Throwable): Response {
        logger.error("Unexpected request failure", exception)
        return localizedErrorResponse(localizer, ErrorCode.INTERNAL_ERROR)
    }
}
