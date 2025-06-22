package ru.grabovsky.dungeoncrusherbot.strategy.context

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.strategy.state.State
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode


@Component
class StateContext(
    private val states: List<State>
) {
    fun next(user: User, stateCode: StateCode) =
        states.firstOrNull { it.isAvailableForCurrentState(stateCode) }?.getNextState(user)
}