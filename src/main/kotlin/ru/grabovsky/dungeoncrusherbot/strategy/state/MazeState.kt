package ru.grabovsky.dungeoncrusherbot.strategy.state

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService

@Component
class MazeState(
    private val stateService: StateService
) : State {
    override fun getNextState(user: User): StateCode? {
        val callback = stateService.getState(user).callbackData
        return when(callback) {
            "REFRESH_MAZE" -> StateCode.CONFIRM_REFRESH_MAZE
            else -> StateCode.UPDATE_MAZE
        }
    }
}