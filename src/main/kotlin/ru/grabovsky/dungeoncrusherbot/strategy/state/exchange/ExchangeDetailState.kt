package ru.grabovsky.dungeoncrusherbot.strategy.state.exchange

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.state.State
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.*

@Component
class ExchangeDetailState(
    private val stateService: StateService
) : State {
    override fun getNextState(user: User): StateCode? {
        val state = stateService.getState(user)
        return when(state.callbackData) {
            "BACK" -> UPDATE_EXCHANGE
            "EXCHANGE_MAP", "EXCHANGE_VOID" -> SET_TARGET_SERVER
            "SELL_MAP", "BUY_MAP" -> SET_SOURCE_PRICE
            "REMOVE_EXCHANGE_REQUEST" -> REMOVE_EXCHANGE_REQUEST
            "SEARCH_EXCHANGE" -> SEARCH_EXCHANGE
            else -> EXCHANGE_DETAIL
        }
    }
}
