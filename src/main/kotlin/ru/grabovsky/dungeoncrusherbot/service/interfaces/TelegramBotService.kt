package ru.grabovsky.dungeoncrusherbot.service.interfaces

import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.entity.UpdateMessage
import ru.grabovsky.dungeoncrusherbot.entity.User as BotUser

interface TelegramBotService {
    fun sendNotification(chatId: Long, type: NotificationType, servers: List<Server> = emptyList(), isBefore: Boolean? = null): Boolean
    fun sendReleaseNotes(user: BotUser, updateMessage: UpdateMessage)
    fun deleteOldNotify()
}
