package ru.grabovsky.dungeoncrusherbot.util

import java.util.*

object LocaleUtils {
    private val defaultLocale = Locale.forLanguageTag("ru")

    fun resolve(language: String?): Locale {
        if (language.isNullOrBlank()) {
            return defaultLocale
        }
        val normalized = language.replace('_', '-').lowercase()
        // try as full language tag first
        val locale = runCatching { Locale.forLanguageTag(normalized) }.getOrNull()
        if (locale != null && locale.language.isNotBlank()) {
            return when (locale.language) {
                "ru" -> Locale.forLanguageTag("ru")
                else -> Locale.forLanguageTag(locale.language)
            }
        }
        return when {
            normalized.startsWith("ru") -> Locale.forLanguageTag("ru")
            else -> Locale.forLanguageTag(normalized.take(2))
        }
    }
}
