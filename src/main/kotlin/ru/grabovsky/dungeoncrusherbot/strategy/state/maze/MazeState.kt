package ru.grabovsky.dungeoncrusherbot.strategy.state.maze

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.state.State
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.*

@Component
class MazeState(
    private val stateService: StateService
) : State {
    override fun getNextState(user: User): StateCode? {
        val callback = stateService.getState(user).callbackData
        return when(callback) {
            "REFRESH_MAZE" -> CONFIRM_REFRESH_MAZE
            "SAME_LEFT" -> SAME_LEFT
            "SAME_RIGHT" -> SAME_RIGHT
            "SAME_CENTER" -> SAME_CENTER
            else -> UPDATE_MAZE
        }
    }
}