package com.example.starter

import com.example.starter.application.auth.PersonalTokens
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.inject.Inject
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
class AuthApiTest {
    @Inject
    lateinit var database: DatabaseTestSupport

    @Inject
    lateinit var personalTokens: PersonalTokens

    @BeforeEach
    fun resetDatabase() = database.reset()

    @Test
    fun `registers a customer without creating a credential`() {
        given()
            .contentType("application/json")
            .body(registerBody())
            .`when`().post("/api/auth/register")
            .then()
            .statusCode(201)
            .body("email", equalTo("user@example.com"))
            .body("role", equalTo("CUSTOMER"))
            .body("accessToken", equalTo(null))
            .body("passwordHash", equalTo(null))
    }

    @Test
    fun `rejects a duplicate email regardless of case`() {
        register()

        given()
            .contentType("application/json")
            .body(registerBody(email = "USER@EXAMPLE.COM"))
            .`when`().post("/api/auth/register")
            .then()
            .statusCode(409)
            .body("code", equalTo("EMAIL_ALREADY_REGISTERED"))
    }

    @Test
    fun `logs in using a trusted spa session and resolves current auth`() {
        register()
        val sessionCookie = login()

        given()
            .header("Origin", TRUSTED_ORIGIN)
            .cookie("app_session", sessionCookie)
            .`when`().get("/api/auth/me")
            .then()
            .statusCode(200)
            .body("type", equalTo("SESSION"))
            .body("user.principalName", equalTo("user@example.com"))
            .body("user.roles", hasItem("CUSTOMER"))
            .body("token", equalTo(null))
    }

    @Test
    fun `rejects invalid login credentials without account disclosure`() {
        register()

        given()
            .header("Origin", TRUSTED_ORIGIN)
            .contentType("application/json")
            .body("""{"email":"user@example.com","password":"wrong-password"}""")
            .`when`().post("/api/auth/login")
            .then()
            .statusCode(401)
            .body("code", equalTo("INVALID_CREDENTIALS"))
    }

    @Test
    fun `login requires a configured browser origin`() {
        register()

        given()
            .contentType("application/json")
            .body(loginBody())
            .`when`().post("/api/auth/login")
            .then()
            .statusCode(403)
            .body("code", equalTo("UNTRUSTED_ORIGIN"))
    }

    @Test
    fun `gate returns unauthorized when no user is authenticated`() {
        given()
            .`when`().get("/api/auth/me")
            .then()
            .statusCode(401)
            .body("code", equalTo("AUTHENTICATION_REQUIRED"))

        given()
            .`when`().get("/api/users")
            .then()
            .statusCode(401)
            .body("code", equalTo("AUTHENTICATION_REQUIRED"))
    }

    @Test
    fun `application gate still enforces user roles`() {
        val userId = register()
        val token = personalTokens.createToken(userId, "role-test", setOf("*")).plainTextToken

        given()
            .auth().oauth2(token)
            .`when`().get("/api/users")
            .then()
            .statusCode(403)
            .body("code", equalTo("FORBIDDEN"))

        database.promoteToAdmin("user@example.com")

        given()
            .auth().oauth2(token)
            .`when`().get("/api/users")
            .then()
            .statusCode(200)
            .body("email", hasItem("user@example.com"))
            .body("[0].passwordHash", equalTo(null))
    }

    private fun register(): Long = given()
        .contentType("application/json")
        .body(registerBody())
        .`when`().post("/api/auth/register")
        .then()
        .statusCode(201)
        .extract().path<Number>("id").toLong()

    private fun login(): String = given()
        .header("Origin", TRUSTED_ORIGIN)
        .contentType("application/json")
        .body(loginBody())
        .`when`().post("/api/auth/login")
        .then()
        .statusCode(200)
        .extract().cookie("app_session")

    private fun registerBody(email: String = "user@example.com") =
        """{"name":"Test User","email":"$email","password":"password123"}"""

    private fun loginBody() = """{"email":"user@example.com","password":"password123"}"""

    private companion object {
        const val TRUSTED_ORIGIN = "http://localhost:3000"
    }
}
