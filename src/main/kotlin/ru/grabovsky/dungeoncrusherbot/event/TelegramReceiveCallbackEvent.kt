package ru.grabovsky.dungeoncrusherbot.event

import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

data class TelegramReceiveCallbackEvent(
    override val user: User,
    override val stateCode: StateCode,
    val callbackData: String
): TelegramEvent