package ru.grabovsky.dungeoncrusherbot.strategy.data

import org.springframework.stereotype.Repository
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.MazeDto

@Repository
class MazeDataRepository(
    private val userService: UserService
): AbstractDataRepository<MazeDto>() {
    override fun getData(user: User): MazeDto {
        val location = userService.getUser(user.id)?.maze?.currentLocation ?: return MazeDto()
        return MazeDto(location)
    }
}