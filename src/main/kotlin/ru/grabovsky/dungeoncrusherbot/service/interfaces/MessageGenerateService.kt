package ru.grabovsky.dungeoncrusherbot.service.interfaces

import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.util.Locale

interface MessageGenerateService {
    fun process(state: StateCode, freemarkerData: Any? = null, locale: Locale = Locale.forLanguageTag("ru")): String
}
