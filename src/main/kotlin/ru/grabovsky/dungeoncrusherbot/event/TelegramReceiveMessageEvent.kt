package ru.grabovsky.dungeoncrusherbot.event

import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

data class TelegramReceiveMessageEvent(
    override val user: User,
    override val stateCode: StateCode,
    val message: Message
): TelegramEvent
