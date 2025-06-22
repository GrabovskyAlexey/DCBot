package ru.grabovsky.dungeoncrusherbot.service.interfaces

import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

interface TelegramBotService {
    fun processState(user: User, stateCode: StateCode)
    fun sendNotification(chatId: Long, servers: List<Server> = emptyList())
}