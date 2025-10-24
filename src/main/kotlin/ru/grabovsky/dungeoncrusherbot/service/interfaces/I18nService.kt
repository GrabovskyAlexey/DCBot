package ru.grabovsky.dungeoncrusherbot.service.interfaces

import java.util.Locale


interface I18nService {
    fun i18n(code: String, locale: Locale, default: String? = null, vararg args: Any?): String
}