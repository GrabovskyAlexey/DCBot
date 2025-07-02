package ru.grabovsky.dungeoncrusherbot.strategy.data

import jakarta.transaction.Transactional
import org.springframework.stereotype.Repository
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.MazeDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.NotifyDto
import ru.grabovsky.dungeoncrusherbot.util.CommonUtils.currentStateCode

@Repository
class NotifyDataRepository(
    private val userService: UserService,
): AbstractDataRepository<NotifyDto>() {
    override fun getData(user: User): NotifyDto {
        val userFromDb = userService.getUser(user.id)
        return NotifyDto(userFromDb?.notificationSubscribe ?: emptyList())
    }
}