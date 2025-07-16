package ru.grabovsky.dungeoncrusherbot.strategy.state

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User

@Component
class ConfirmRefreshMazeState() : State {
    override fun getNextState(user: User): StateCode? = StateCode.UPDATE_MAZE
}