package com.example.starter.http.localization

import io.vertx.core.http.HttpHeaders
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.RequestScoped
import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle

@RequestScoped
class Localizer(private val routingContext: RoutingContext) {
    val locale: Locale by lazy(::resolveLocale)

    val languageTag: String
        get() = locale.toLanguageTag()

    fun message(key: String, vararg arguments: Any): String {
        val pattern = translatedPattern(key)
        return MessageFormat(pattern, locale).format(arguments)
    }

    private fun translatedPattern(key: String): String {
        val localizedBundle = ResourceBundle.getBundle(BUNDLE_NAME, locale)
        if (localizedBundle.containsKey(key)) return localizedBundle.getString(key)

        val defaultBundle = ResourceBundle.getBundle(BUNDLE_NAME, DEFAULT_LOCALE)
        return if (defaultBundle.containsKey(key)) defaultBundle.getString(key) else key
    }

    private fun resolveLocale(): Locale {
        val acceptLanguage = routingContext.request()
            .getHeader(HttpHeaders.ACCEPT_LANGUAGE)
            ?: return DEFAULT_LOCALE

        return runCatching {
            Locale.lookup(Locale.LanguageRange.parse(acceptLanguage), SUPPORTED_LOCALES)
        }.getOrNull() ?: DEFAULT_LOCALE
    }

    private companion object {
        const val BUNDLE_NAME = "messages"
        val DEFAULT_LOCALE: Locale = Locale.ENGLISH
        val SUPPORTED_LOCALES: List<Locale> = listOf(DEFAULT_LOCALE, Locale.forLanguageTag("ar"))
    }
}
