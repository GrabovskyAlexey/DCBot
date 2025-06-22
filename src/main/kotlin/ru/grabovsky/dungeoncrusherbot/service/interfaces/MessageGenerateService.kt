package ru.grabovsky.dungeoncrusherbot.service.interfaces

import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

interface MessageGenerateService {
    fun process(state: StateCode, freemarkerData: Any?): String
}