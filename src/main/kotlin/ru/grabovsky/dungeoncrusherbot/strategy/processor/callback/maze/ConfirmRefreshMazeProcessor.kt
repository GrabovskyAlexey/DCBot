package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.maze

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.Maze
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MazeService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.CallbackProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.ExecuteStatus

@Component
class ConfirmRefreshMazeProcessor(
    private val userService: UserService,
    private val mazeService: MazeService,
    stateService: StateService
) : CallbackProcessor(stateService) {
    override fun process(
        user: User,
        callbackData: String
    ): ExecuteStatus {
        val userFromDb = userService.getUser(user.id)!!
        val maze = userFromDb.maze ?: Maze(user = userFromDb)
        when (callbackData) {
            "CONFIRM" -> mazeService.refreshMaze(maze)
            else -> {}
        }

        return ExecuteStatus.FINAL
    }
}