package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.entity.Location
import ru.grabovsky.dungeoncrusherbot.entity.Maze
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MazeService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Component
class MazeProcessor(
    private val userService: UserService,
    private val mazeService: MazeService
) : CallbackProcessor {
    override fun execute(
        user: User,
        callbackQuery: CallbackQuery
    ): ExecuteStatus {
        val user = userService.getUser(user.id)!!
        val maze = user.maze ?: Maze(user = user)
        val startLocation = maze.currentLocation ?: Location(0, 0, Direction.CENTER)
        when (callbackQuery.data) {
            "LEFT" -> mazeService.processStep(maze, Direction.LEFT)
            "RIGHT" -> mazeService.processStep(maze, Direction.RIGHT)
            "CENTER" -> mazeService.processStep(maze, Direction.CENTER)
        }

        return ExecuteStatus.FINAL
    }
}