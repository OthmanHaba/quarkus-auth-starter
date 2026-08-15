package com.example.starter.internal.auth.cookie

import com.example.starter.internal.auth.config.AuthConfig
import io.vertx.core.http.Cookie
import io.vertx.core.http.CookieSameSite
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class AuthCookieManager(private val config: AuthConfig) {
    fun setSession(context: RoutingContext, value: String) {
        context.response().addCookie(baseCookie(config.sessionCookieName(), value)
            .setHttpOnly(true)
            .setMaxAge(config.sessionDuration().seconds))
    }

    fun clearSession(context: RoutingContext) {
        context.response().addCookie(baseCookie(config.sessionCookieName(), "")
            .setHttpOnly(true)
            .setMaxAge(0))
    }

    fun setCsrf(context: RoutingContext, value: String) {
        context.response().addCookie(baseCookie(config.csrfCookieName(), value)
            .setHttpOnly(false)
            .setMaxAge(config.sessionDuration().seconds))
    }

    private fun baseCookie(name: String, value: String): Cookie {
        val cookie = Cookie.cookie(name, value)
            .setPath("/")
            .setSecure(config.cookieSecure())
            .setSameSite(CookieSameSite.valueOf(config.cookieSameSite().uppercase()))
        config.cookieDomain().filter(String::isNotBlank).ifPresent(cookie::setDomain)
        return cookie
    }
}
