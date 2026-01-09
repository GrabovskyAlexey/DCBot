package ru.grabovsky.dungeoncrusherbot.service.interfaces

import ru.grabovsky.dungeoncrusherbot.entity.User
import org.telegram.telegrambots.meta.api.objects.User as TgUser

interface UserService {
    fun createOrUpdateUser(user: TgUser): User
    fun saveUser(user: User)
    fun getUser(userId: Long): User?
    fun updateBlockedStatus(userId: Long, isBlocked: Boolean)
    fun clearNotes(user: TgUser)
    fun addNote(userId: Long, note: String): Boolean
    fun removeNote(userId: Long, index: Int): Boolean
    fun sendAdminMessage(user: TgUser, message: String, sourceMessageId: Int, sourceChatId: Long)
    fun sendAdminReply(admin: TgUser, targetUserId: Long, message: String, replyToMessageId: Int?)
    fun findByUsername(username: String): User?
}
