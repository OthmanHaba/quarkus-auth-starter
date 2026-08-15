package com.example.starter.shared

enum class ErrorCode(
    val status: Int,
    val messageKey: String,
) {
    VALIDATION_ERROR(422, "api.error.validation"),
    MALFORMED_REQUEST(400, "api.error.malformed_request"),
    AUTHENTICATION_REQUIRED(401, "api.error.authentication_required"),
    INVALID_CREDENTIALS(401, "api.error.invalid_credentials"),
    FORBIDDEN(403, "api.error.forbidden"),
    EMAIL_ALREADY_REGISTERED(409, "api.error.email_already_registered"),
    TOKEN_NOT_FOUND(404, "api.error.token_not_found"),
    SESSION_NOT_FOUND(404, "api.error.session_not_found"),
    UNTRUSTED_ORIGIN(403, "api.error.untrusted_origin"),
    CSRF_TOKEN_MISMATCH(419, "api.error.csrf_token_mismatch"),
    INTERNAL_ERROR(500, "api.error.internal"),
}
