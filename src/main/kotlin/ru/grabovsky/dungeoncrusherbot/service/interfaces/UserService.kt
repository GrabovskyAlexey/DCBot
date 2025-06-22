package ru.grabovsky.dungeoncrusherbot.service.interfaces

import ru.grabovsky.dungeoncrusherbot.entity.User
import org.telegram.telegrambots.meta.api.objects.User as TgUser

interface UserService {
    fun createOrUpdateUser(user: TgUser): User
    fun saveUser(user: User)
    fun getUser(userId: Long): User?
}