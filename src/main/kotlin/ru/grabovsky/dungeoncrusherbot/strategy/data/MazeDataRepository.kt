package ru.grabovsky.dungeoncrusherbot.strategy.data

import org.springframework.stereotype.Repository
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.MazeDto
import ru.grabovsky.dungeoncrusherbot.util.CommonUtils.currentStateCode

@Repository
class MazeDataRepository(
    private val userService: UserService,
    private val stateService: StateService
): AbstractDataRepository<MazeDto>() {
    override fun getData(user: User): MazeDto {
        val userFromDb = userService.getUser(user.id)
        val location = userFromDb?.maze?.currentLocation ?: return MazeDto()
        val maze = userFromDb.maze!!
        val state = stateService.getState(user)
        if (state.callbackData == "HISTORY") {
            stateService.updateState(user, currentStateCode("DataRepository"))
            maze.steps.takeLast(20).map {
                "${it.startLocation.level} "
            }
            return MazeDto(location, maze.steps.takeLast(20).map { it.toString() })
        }
        return MazeDto(location)
    }
}