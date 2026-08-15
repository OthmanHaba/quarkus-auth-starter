package com.example.starter

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.inject.Inject
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
class LocalizationApiTest {
    @Inject
    lateinit var database: DatabaseTestSupport

    @BeforeEach
    fun resetDatabase() = database.reset()

    @Test
    fun `localizes api exceptions from accept language`() {
        given()
            .header("Accept-Language", "ar-LY,ar;q=0.9,en;q=0.8")
            .`when`().get("/api/auth/me")
            .then()
            .statusCode(401)
            .header("Content-Language", equalTo("ar"))
            .body("code", equalTo("AUTHENTICATION_REQUIRED"))
            .body("message", equalTo("المصادقة مطلوبة."))
    }

    @Test
    fun `localizes validation errors from accept language`() {
        given()
            .header("Accept-Language", "ar")
            .contentType("application/json")
            .body(
                """
                {"name":"Test User","email":"not-an-email","password":"password123"}
                """.trimIndent(),
            )
            .`when`().post("/api/auth/register")
            .then()
            .statusCode(422)
            .header("Content-Language", equalTo("ar"))
            .body("message", equalTo("فشل التحقق من صحة الطلب."))
            .body("details.message", hasItem("يجب إدخال بريد إلكتروني صالح."))
    }
}
