package ru.grabovsky.dungeoncrusherbot.strategy.state.exchange

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.state.State
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

@Component
class ExchangeState(
    private val stateService: StateService
) : State {
    override fun getNextState(user: User): StateCode? {
        val state = stateService.getState(user)
        return when {
            state.state == StateCode.UPDATE_EXCHANGE -> StateCode.UPDATE_EXCHANGE
            state.callbackData?.startsWith("DETAIL:") == true -> when (state.state) {
                StateCode.EXCHANGE_DETAIL -> StateCode.EXCHANGE_DETAIL
                StateCode.UPDATE_EXCHANGE_DETAIL -> StateCode.UPDATE_EXCHANGE_DETAIL
                else -> null
            }
            else -> null
        }
    }
}