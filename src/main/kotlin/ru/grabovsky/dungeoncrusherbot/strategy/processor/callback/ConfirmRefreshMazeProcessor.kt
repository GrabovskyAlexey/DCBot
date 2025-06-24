package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.entity.Location
import ru.grabovsky.dungeoncrusherbot.entity.Maze
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MazeService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Component
class ConfirmRefreshMazeProcessor(
    private val userService: UserService,
    private val mazeService: MazeService,
    stateService: StateService
) : CallbackProcessor(stateService) {
    override fun process(
        user: User,
        callbackQuery: CallbackQuery
    ): ExecuteStatus {
        val user = userService.getUser(user.id)!!
        val maze = user.maze ?: Maze(user = user)
        when (callbackQuery.data) {
            "REFRESH_MAZE_CONFIRM" -> mazeService.refreshMaze(maze)
            else -> {}
        }

        return ExecuteStatus.FINAL
    }
}