package com.example.starter

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test

@QuarkusTest
class ApiDocsTest {
    @Test
    fun `scalar reference is served outside production`() {
        given()
            .`when`().get("/api-docs")
            .then().statusCode(200)
            .contentType(containsString("text/html"))
            .body(containsString("@scalar/api-reference"))
            .body(containsString("/q/openapi"))
    }

    @Test
    fun `openapi document covers every auth endpoint`() {
        val paths = given()
            .accept("application/json")
            .`when`().get("/q/openapi")
            .then().statusCode(200)
            .extract().jsonPath()

        // Also guards against the OpenApiConfiguration Application subclass
        // silently disabling JAX-RS resource discovery.
        listOf(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/me",
            "/api/auth/csrf-cookie",
            "/api/auth/tokens",
            "/api/auth/tokens/{tokenId}",
            "/api/auth/sessions",
            "/api/auth/sessions/{sessionId}",
            "/api/auth/sessions/others",
            "/api/users",
        ).forEach { path ->
            require(paths.get<Any?>("paths.'$path'") != null) { "missing $path in OpenAPI document" }
        }

        // The docs page itself is not part of the API.
        require(paths.get<Any?>("paths.'/api-docs'") == null) { "/api-docs should be hidden" }
    }

    @Test
    fun `both authentication mechanisms are documented`() {
        given()
            .accept("application/json")
            .`when`().get("/q/openapi")
            .then().statusCode(200)
            .body("components.securitySchemes.personalToken.scheme", org.hamcrest.Matchers.equalTo("bearer"))
            .body("components.securitySchemes.sessionCookie.in", org.hamcrest.Matchers.equalTo("cookie"))
            .body("components.securitySchemes.sessionCookie.name", org.hamcrest.Matchers.equalTo("app_session"))
    }
}
