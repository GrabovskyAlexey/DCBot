package ru.grabovsky.dungeoncrusherbot.strategy.data.settings

import org.springframework.stereotype.Repository
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.data.AbstractDataRepository
import ru.grabovsky.dungeoncrusherbot.strategy.dto.SettingsDto

@Repository
class SettingsDataRepository(
    private val userService: UserService,
): AbstractDataRepository<SettingsDto>() {
    override fun getData(user: User): SettingsDto {
        val userFromDb = userService.getUser(user.id)
        return SettingsDto(
            userFromDb?.notificationSubscribe?.firstOrNull { it.type == NotificationType.SIEGE}?.enabled == true,
            userFromDb?.notificationSubscribe?.firstOrNull{ it.type == NotificationType.MINE}?.enabled == true,
            userFromDb?.settings?.resourcesCb ?: false
        )
    }
}