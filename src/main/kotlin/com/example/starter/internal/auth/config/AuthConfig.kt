package com.example.starter.internal.auth.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import io.quarkus.runtime.annotations.StaticInitSafe
import java.time.Duration
import java.util.Optional

@StaticInitSafe
@ConfigMapping(prefix = "app.auth")
interface AuthConfig {
    fun statefulOrigins(): List<String>
    fun sessionCookieName(): String
    fun csrfCookieName(): String
    fun sessionDuration(): Duration
    fun tokenDefaultExpiration(): Duration
    fun lastUsedUpdateInterval(): Duration
    fun revokedRetention(): Duration
    fun cookieSecure(): Boolean

    @WithDefault("LAX")
    fun cookieSameSite(): String

    @WithDefault("")
    fun cookieDomain(): Optional<String>
}
