package com.example.starter.http.docs

import jakarta.ws.rs.ApplicationPath
import jakarta.ws.rs.core.Application
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme

/**
 * Declares how the API is authenticated so the generated document is usable, not
 * just a list of paths. Title, version and description come from
 * `quarkus.smallrye-openapi.info-*` so downstream applications can rebrand
 * without editing Kotlin.
 *
 * `getClasses()` is deliberately not overridden: Quarkus keeps scanning for
 * resources, and this class only carries annotations.
 */
@ApplicationPath("/")
@OpenAPIDefinition(
    info = org.eclipse.microprofile.openapi.annotations.info.Info(title = "", version = ""),
)
@SecurityScheme(
    securitySchemeName = "personalToken",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    description = "Opaque personal access token in `<token-id>|<secret>` form. " +
        "Create one at POST /api/auth/tokens; the plaintext is returned only once.",
)
@SecurityScheme(
    securitySchemeName = "sessionCookie",
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.COOKIE,
    apiKeyName = "app_session",
    description = "Server-side SPA session cookie set by POST /api/auth/login. " +
        "Requires a trusted Origin, and mutations additionally require the " +
        "X-XSRF-TOKEN header (see GET /api/auth/csrf-cookie).",
)
class OpenApiConfiguration : Application()
