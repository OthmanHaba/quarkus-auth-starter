package com.example.starter.internal.auth.config

import io.vertx.core.http.HttpHeaders
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped
import java.net.URI

@ApplicationScoped
class StatefulOriginMatcher(config: AuthConfig) {
    private val trustedOrigins = config.statefulOrigins().mapNotNull(::parseOrigin).toSet()

    fun isTrusted(context: RoutingContext): Boolean {
        val request = context.request()
        val candidate = request.getHeader(HttpHeaders.ORIGIN)
            ?: request.getHeader(HttpHeaders.REFERER)
            ?: return false
        return parseOrigin(candidate) in trustedOrigins
    }

    private fun parseOrigin(value: String): Origin? = runCatching {
        val uri = URI(value.trim())
        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (uri.userInfo != null) return null
        Origin(scheme, host, effectivePort(scheme, uri.port))
    }.getOrNull()

    private fun effectivePort(scheme: String, port: Int): Int = when {
        port >= 0 -> port
        scheme == "https" -> 443
        else -> 80
    }

    private data class Origin(val scheme: String, val host: String, val port: Int)
}
