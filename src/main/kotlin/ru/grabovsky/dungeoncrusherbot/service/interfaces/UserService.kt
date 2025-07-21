package ru.grabovsky.dungeoncrusherbot.service.interfaces

import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

interface UserService {
    fun createOrUpdateUser(user: TgUser): User
    fun saveUser(user: User)
    fun getUser(userId: Long): User?
    fun processNote(user: User, note: String, state: StateCode)
    fun clearNotes(user: TgUser)
    fun sendAdminMessage(user: TgUser, message: String)
}