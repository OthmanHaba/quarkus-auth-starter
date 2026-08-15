package com.example.starter.http.docs

import io.quarkus.arc.profile.UnlessBuildProfile
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.Operation

/**
 * Scalar API reference over the generated OpenAPI document.
 *
 * `@UnlessBuildProfile("prod")` removes the bean at build time, so a production
 * build has no /api-docs route at all -- not a route that returns 403. The
 * document itself is switched off separately by
 * `%prod.quarkus.smallrye-openapi.enable=false`.
 */
@Path("/api-docs")
@UnlessBuildProfile("prod")
class ApiDocsResource {
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Operation(hidden = true)
    fun scalar(): String = PAGE

    private companion object {
        // ponytail: the OpenAPI path is Quarkus's default (non-application root
        // /q + smallrye-openapi path). Change both together if you move it.
        const val OPENAPI_PATH = "/q/openapi"

        val PAGE = """
            <!doctype html>
            <html>
              <head>
                <title>API Reference</title>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
              </head>
              <body>
                <div id="app"></div>
                <script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference"></script>
                <script>
                  Scalar.createApiReference('#app', { url: '$OPENAPI_PATH' })
                </script>
              </body>
            </html>
        """.trimIndent()
    }
}
