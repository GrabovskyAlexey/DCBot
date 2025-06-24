package ru.grabovsky.dungeoncrusherbot.strategy.state

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService

@Component
class ConfirmRefreshMazeState() : State {
    override fun getNextState(user: User): StateCode? = StateCode.UPDATE_MAZE
}