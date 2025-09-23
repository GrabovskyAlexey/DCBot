package ru.grabovsky.dungeoncrusherbot.service.interfaces

import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.entity.UpdateMessage
import ru.grabovsky.dungeoncrusherbot.strategy.dto.AdminMessageDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.entity.User as BotUser

interface TelegramBotService {
    fun processState(user: User, stateCode: StateCode)
    fun sendNotification(chatId: Long, type: NotificationType, servers: List<Server> = emptyList(), isBefore: Boolean? = null): Boolean
    fun sendAdminMessage(chatId: Long, dto: AdminMessageDto)
    fun sendReleaseNotes(user: BotUser, updateMessage: UpdateMessage)
    fun deleteOldNotify()
}
