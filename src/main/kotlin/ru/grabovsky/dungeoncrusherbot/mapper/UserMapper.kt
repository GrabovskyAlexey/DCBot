package ru.grabovsky.dungeoncrusherbot.mapper

import ru.grabovsky.dungeoncrusherbot.entity.User
import org.telegram.telegrambots.meta.api.objects.User as TgUser

object UserMapper {
    fun fromTelegramToEntity(user: TgUser) =
        User(
            userId = user.id,
            firstName = user.firstName,
            lastName = user.lastName,
            userName = user.userName,
        )

}