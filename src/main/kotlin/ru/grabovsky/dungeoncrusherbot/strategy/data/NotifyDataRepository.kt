package ru.grabovsky.dungeoncrusherbot.strategy.data

import org.springframework.stereotype.Repository
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.NotifyDto

@Repository
class NotifyDataRepository(
    private val userService: UserService,
): AbstractDataRepository<NotifyDto>() {
    override fun getData(user: User): NotifyDto {
        val userFromDb = userService.getUser(user.id)
        return NotifyDto(
            userFromDb?.notificationSubscribe?.firstOrNull { it.type == NotificationType.SIEGE}?.enabled == true,
            userFromDb?.notificationSubscribe?.firstOrNull{ it.type == NotificationType.MINE}?.enabled == true,
        )
    }
}