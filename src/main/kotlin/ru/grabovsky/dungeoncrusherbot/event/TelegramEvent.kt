package ru.grabovsky.dungeoncrusherbot.event

import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

interface TelegramEvent {
    val user: User
    val stateCode: StateCode
}