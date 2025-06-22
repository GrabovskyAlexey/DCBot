package ru.grabovsky.dungeoncrusherbot.event

import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

data class TelegramStateEvent(
    override val user: User,
    override val stateCode: StateCode
): TelegramEvent
