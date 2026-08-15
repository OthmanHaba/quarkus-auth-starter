package com.example.starter

import com.example.starter.application.auth.PersonalTokens
import com.example.starter.application.auth.SessionManager
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.response.Response
import jakarta.inject.Inject
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
class TokenAuthTest {
    @Inject
    lateinit var database: DatabaseTestSupport

    @Inject
    lateinit var personalTokens: PersonalTokens

    @Inject
    lateinit var sessions: SessionManager

    @BeforeEach
    fun resetDatabase() = database.reset()

    @Test
    fun `personal tokens use id and secret format and persist only the hash`() {
        val userId = register()
        val created = personalTokens.createToken(userId, "mobile", setOf("profile:read"))
        val (idPart, secret) = created.plainTextToken.split("|", limit = 2)

        assertEquals(created.accessToken.id.toString(), idPart)
        assertTrue(secret.matches(Regex("[A-Za-z0-9_-]{43}")))
        assertTrue(database.tokenHash(created.accessToken.id).matches(Regex("[a-f0-9]{64}")))
        assertNotEquals(secret, database.tokenHash(created.accessToken.id))
        assertFalse(created.accessToken.can("users:manage"))
        assertTrue(created.accessToken.can("profile:read"))
    }

    @Test
    fun `valid bearer token authenticates and exposes safe current auth`() {
        val token = token(register(), setOf("profile:read"))

        given()
            .auth().oauth2(token)
            .`when`().get("/api/auth/me")
            .then()
            .statusCode(200)
            .body("type", equalTo("PERSONAL_ACCESS_TOKEN"))
            .body("user.principalName", equalTo(DEFAULT_EMAIL))
            .body("token.abilities", hasSize<Any>(1))
            .body("token.tokenHash", equalTo(null))
            .body("session", equalTo(null))
    }

    @Test
    fun `malformed unknown incorrect expired and revoked bearer tokens return unauthorized`() {
        val userId = register()
        val created = personalTokens.createToken(userId, "invalid-cases", setOf("*"))

        listOf("malformed", "999999|secret", "${created.accessToken.id}|wrong-secret").forEach { credential ->
            given().auth().oauth2(credential)
                .`when`().get("/api/auth/me")
                .then().statusCode(401)
        }

        database.expireToken(created.accessToken.id)
        given().auth().oauth2(created.plainTextToken)
            .`when`().get("/api/auth/me")
            .then().statusCode(401)

        val revoked = personalTokens.createToken(userId, "revoked", setOf("*"))
        personalTokens.revokeToken(userId, revoked.accessToken.id)
        given().auth().oauth2(revoked.plainTextToken)
            .`when`().get("/api/auth/me")
            .then().statusCode(401)
    }

    @Test
    fun `inactive or deleted token owners cannot authenticate`() {
        val inactiveUser = register()
        val inactiveToken = token(inactiveUser, setOf("*"))
        database.deactivateUser(inactiveUser)

        given().auth().oauth2(inactiveToken)
            .`when`().get("/api/auth/me")
            .then().statusCode(401)

        database.reset()
        val deletedUser = register()
        val deletedToken = token(deletedUser, setOf("*"))
        database.deleteUser(deletedUser)

        given().auth().oauth2(deletedToken)
            .`when`().get("/api/auth/me")
            .then().statusCode(401)
    }

    @Test
    fun `token abilities support wildcard any class scope and method override`() {
        val userId = register()
        val readToken = token(userId, setOf("tokens:read"))
        val deniedToken = token(userId, setOf("profile:read"))
        val sessionReadToken = token(userId, setOf("sessions:read"))
        val sessionRevokeToken = token(userId, setOf("sessions:revoke"))
        val wildcardToken = token(userId, setOf("*"))
        val browserSession = login()

        given().auth().oauth2(readToken)
            .`when`().get("/api/auth/tokens")
            .then().statusCode(200)
        given().auth().oauth2(deniedToken)
            .`when`().get("/api/auth/tokens")
            .then().statusCode(403)
        given().auth().oauth2(sessionReadToken)
            .`when`().get("/api/auth/sessions")
            .then().statusCode(200)
        given().auth().oauth2(sessionRevokeToken)
            .`when`().delete("/api/auth/sessions/${browserSession.id}")
            .then().statusCode(204)
        given().auth().oauth2(wildcardToken)
            .`when`().get("/api/auth/tokens")
            .then().statusCode(200)
        // Wildcard must also satisfy Quarkus @PermissionsAllowed, not just @TokenAbilities.
        given().auth().oauth2(wildcardToken)
            .`when`().get("/api/auth/sessions")
            .then().statusCode(200)
    }

    @Test
    fun `token creation requires its ability while bearer authentication needs no csrf`() {
        val userId = register()
        val denied = token(userId, setOf("tokens:read"))
        val allowed = token(userId, setOf("tokens:create"))

        given().auth().oauth2(denied)
            .contentType("application/json")
            .body(createTokenBody())
            .`when`().post("/api/auth/tokens")
            .then().statusCode(403)
            .body("code", equalTo("FORBIDDEN"))

        given().auth().oauth2(allowed)
            .contentType("application/json")
            .body(createTokenBody())
            .`when`().post("/api/auth/tokens")
            .then().statusCode(201)
    }

    @Test
    fun `token management returns plaintext once and never exposes hashes`() {
        val userId = register()
        val bootstrap = token(userId, setOf("*"))

        val creationResponse = given().auth().oauth2(bootstrap)
            .contentType("application/json")
            .body("""{"name":"cli","abilities":["profile:read"]}""")
            .`when`().post("/api/auth/tokens")
            .then().statusCode(201)
            .body("plainTextToken", not(equalTo(null)))
            .body("accessToken.name", equalTo("cli"))
            .body("accessToken.tokenHash", equalTo(null))
            .extract().response()

        val createdId = creationResponse.path<Number>("accessToken.id").toLong()
        given().auth().oauth2(bootstrap)
            .`when`().get("/api/auth/tokens")
            .then().statusCode(200)
            .body("name", org.hamcrest.Matchers.hasItem("cli"))

        val listBody = given().auth().oauth2(bootstrap)
            .`when`().get("/api/auth/tokens")
            .then().statusCode(200)
            .extract().asString()
        assertFalse(listBody.contains("tokenHash"))
        assertFalse(listBody.contains("plainTextToken"))
        assertFalse(listBody.contains(creationResponse.path<String>("plainTextToken")))

        given().auth().oauth2(bootstrap)
            .`when`().delete("/api/auth/tokens/$createdId")
            .then().statusCode(204)
    }

    @Test
    fun `one user cannot revoke another users token`() {
        val ownerId = register("owner@example.com")
        val attackerId = register("attacker@example.com")
        val ownerToken = personalTokens.createToken(ownerId, "owner", setOf("*")).accessToken
        val attackerToken = token(attackerId, setOf("*"))

        given().auth().oauth2(attackerToken)
            .`when`().delete("/api/auth/tokens/${ownerToken.id}")
            .then().statusCode(404)
            .body("code", equalTo("TOKEN_NOT_FOUND"))
    }

    @Test
    fun `trusted session authenticates while untrusted and deceptive origins are ignored`() {
        register()
        val login = login()

        given().header("Origin", TRUSTED_ORIGIN).cookie("app_session", login.cookie)
            .`when`().get("/api/auth/me")
            .then().statusCode(200)
            .body("type", equalTo("SESSION"))

        given().cookie("app_session", login.cookie)
            .`when`().get("/api/auth/me")
            .then().statusCode(401)
            .body("code", equalTo("AUTHENTICATION_REQUIRED"))

        listOf("https://evil.example.com", "http://localhost:3000.evil.example").forEach { origin ->
            given().header("Origin", origin).cookie("app_session", login.cookie)
                .`when`().get("/api/auth/me")
                .then().statusCode(403)
        }
    }

    @Test
    fun `expired and revoked sessions return unauthorized`() {
        register()
        val expired = login()
        database.expireSession(expired.id)

        given().header("Origin", TRUSTED_ORIGIN).cookie("app_session", expired.cookie)
            .`when`().get("/api/auth/me")
            .then().statusCode(401)

        val revoked = login()
        sessions.revoke(database.userId(DEFAULT_EMAIL), revoked.id)
        given().header("Origin", TRUSTED_ORIGIN).cookie("app_session", revoked.cookie)
            .`when`().get("/api/auth/me")
            .then().statusCode(401)
    }

    @Test
    fun `session has priority and an invalid trusted session never falls back to bearer`() {
        val userId = register()
        val session = login()
        val bearer = token(userId, setOf("profile:read"))

        given().header("Origin", TRUSTED_ORIGIN)
            .cookie("app_session", session.cookie)
            .auth().oauth2(bearer)
            .`when`().get("/api/auth/me")
            .then().statusCode(200)
            .body("type", equalTo("SESSION"))

        given().header("Origin", TRUSTED_ORIGIN)
            .cookie("app_session", "${session.id}|wrong-secret")
            .auth().oauth2(bearer)
            .`when`().get("/api/auth/me")
            .then().statusCode(401)
    }

    @Test
    fun `session csrf is required and valid double submit succeeds`() {
        register()
        val session = login()

        given().header("Origin", TRUSTED_ORIGIN)
            .cookie("app_session", session.cookie)
            .contentType("application/json")
            .body(createTokenBody())
            .`when`().post("/api/auth/tokens")
            .then().statusCode(419)
            .body("code", equalTo("CSRF_TOKEN_MISMATCH"))

        val csrf = csrf(session.cookie)
        given().header("Origin", TRUSTED_ORIGIN)
            .cookie("app_session", session.cookie)
            .cookie("XSRF-TOKEN", csrf)
            .header("X-XSRF-TOKEN", "invalid-$csrf")
            .contentType("application/json")
            .body(createTokenBody())
            .`when`().post("/api/auth/tokens")
            .then().statusCode(419)

        given().header("Origin", TRUSTED_ORIGIN)
            .cookie("app_session", session.cookie)
            .cookie("XSRF-TOKEN", csrf)
            .header("X-XSRF-TOKEN", csrf)
            .contentType("application/json")
            .body(createTokenBody())
            .`when`().post("/api/auth/tokens")
            .then().statusCode(201)
    }

    @Test
    fun `logout revokes session clears cookie and login rotates an existing session`() {
        register()
        val first = login()
        val firstCsrf = csrf(first.cookie)
        val rotatedResponse = given().header("Origin", TRUSTED_ORIGIN)
            .cookie("app_session", first.cookie)
            .cookie("XSRF-TOKEN", firstCsrf)
            .header("X-XSRF-TOKEN", firstCsrf)
            .contentType("application/json")
            .body(loginBody(DEFAULT_EMAIL))
            .`when`().post("/api/auth/login")
            .then().statusCode(200)
            .extract().response()
        val second = LoginSession(
            rotatedResponse.getCookie("app_session"),
            UUID.fromString(rotatedResponse.path("session.id")),
        )

        assertNotEquals(first.cookie, second.cookie)
        assertTrue(database.isSessionRevoked(first.id))

        val secondCsrf = csrf(second.cookie)
        val logoutResponse = given().header("Origin", TRUSTED_ORIGIN)
            .cookie("app_session", second.cookie)
            .cookie("XSRF-TOKEN", secondCsrf)
            .header("X-XSRF-TOKEN", secondCsrf)
            .contentType("application/json")
            .`when`().post("/api/auth/logout")
            .then().statusCode(204)
            .extract().response()

        assertTrue(database.isSessionRevoked(second.id))
        assertTrue(logoutResponse.headers.getValues("Set-Cookie").any {
            it.contains("app_session=") && it.contains("Max-Age=0")
        })
    }

    @Test
    fun `session management lists safe metadata and revokes only other sessions`() {
        register()
        val first = login()
        val current = login()

        val listBody = given().header("Origin", TRUSTED_ORIGIN)
            .cookie("app_session", current.cookie)
            .`when`().get("/api/auth/sessions")
            .then().statusCode(200)
            .body("id", hasSize<Any>(2))
            .extract().asString()
        assertFalse(listBody.contains("sessionHash"))
        assertFalse(listBody.contains("plainTextSession"))

        val csrf = csrf(current.cookie)
        given().header("Origin", TRUSTED_ORIGIN)
            .cookie("app_session", current.cookie)
            .cookie("XSRF-TOKEN", csrf)
            .header("X-XSRF-TOKEN", csrf)
            .`when`().delete("/api/auth/sessions/others")
            .then().statusCode(204)

        assertTrue(database.isSessionRevoked(first.id))
        assertFalse(database.isSessionRevoked(current.id))
    }

    @Test
    fun `session passes token abilities keeps role checks and throttles last used updates`() {
        register()
        val session = login()
        val before = database.sessionLastUsedEpochMillis(session.id)

        given().header("Origin", TRUSTED_ORIGIN).cookie("app_session", session.cookie)
            .`when`().get("/api/auth/tokens")
            .then().statusCode(200)
        given().header("Origin", TRUSTED_ORIGIN).cookie("app_session", session.cookie)
            .`when`().get("/api/users")
            .then().statusCode(403)

        val after = database.sessionLastUsedEpochMillis(session.id)
        assertEquals(before, after)
    }

    @Test
    fun `cors allows configured credentialed origin without wildcard`() {
        given().header("Origin", TRUSTED_ORIGIN)
            .header("Access-Control-Request-Method", "GET")
            .`when`().options("/api/auth/me")
            .then().statusCode(200)
            .header("Access-Control-Allow-Origin", equalTo(TRUSTED_ORIGIN))
            .header("Access-Control-Allow-Credentials", equalTo("true"))
    }

    private fun register(email: String = DEFAULT_EMAIL): Long = given()
        .contentType("application/json")
        .body("""{"name":"Test User","email":"$email","password":"password123"}""")
        .`when`().post("/api/auth/register")
        .then().statusCode(201)
        .extract().path<Number>("id").toLong()

    private fun token(userId: Long, abilities: Set<String>): String =
        personalTokens.createToken(userId, "test-${System.nanoTime()}", abilities).plainTextToken

    private fun login(email: String = DEFAULT_EMAIL): LoginSession {
        val response: Response = given().header("Origin", TRUSTED_ORIGIN)
            .contentType("application/json")
            .body(loginBody(email))
            .`when`().post("/api/auth/login")
            .then().statusCode(200)
            .extract().response()
        return LoginSession(
            cookie = response.getCookie("app_session"),
            id = UUID.fromString(response.path("session.id")),
        )
    }

    private fun csrf(sessionCookie: String): String = given().header("Origin", TRUSTED_ORIGIN)
        .cookie("app_session", sessionCookie)
        .`when`().get("/api/auth/csrf-cookie")
        .then().statusCode(204)
        .extract().cookie("XSRF-TOKEN")

    private fun loginBody(email: String) =
        """{"email":"$email","password":"password123"}"""

    private fun createTokenBody() = """{"name":"session-created","abilities":["profile:read"]}"""

    private data class LoginSession(val cookie: String, val id: UUID)

    private companion object {
        const val TRUSTED_ORIGIN = "http://localhost:3000"
        const val DEFAULT_EMAIL = "tokens@example.com"
    }
}
