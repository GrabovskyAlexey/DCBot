package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.entity.Maze
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MazeService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.*

@Component
class MazeProcessor(
    private val userService: UserService,
    private val mazeService: MazeService,
    stateService: StateService,
) : CallbackProcessor(stateService) {
    override fun process(
        user: User,
        callbackData: String
    ): ExecuteStatus {
        val userFromDb = userService.getUser(user.id)!!
        val maze = userFromDb.maze ?: Maze(user = userFromDb)
        val state = stateService.getState(user)
        state.prevState = UPDATE_MAZE
        stateService.saveState(state)
        when (callbackData) {
            "LEFT" -> mazeService.processStep(maze, Direction.LEFT)
            "RIGHT" -> mazeService.processStep(maze, Direction.RIGHT)
            "CENTER" -> mazeService.processStep(maze, Direction.CENTER)
            "SAME_STEPS" -> mazeService.revertSameSteps(maze)
        }

        return ExecuteStatus.FINAL
    }
}