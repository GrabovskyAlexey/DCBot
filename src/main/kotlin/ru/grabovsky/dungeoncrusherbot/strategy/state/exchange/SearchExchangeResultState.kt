package ru.grabovsky.dungeoncrusherbot.strategy.state.exchange

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.state.State
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.*

@Component
class SearchExchangeResultState(
    private val stateService: StateService
): State {
    override fun getNextState(user: User): StateCode? {
        val state = stateService.getState(user)
        return when(state.callbackData) {
            "BACK" -> EXCHANGE_DETAIL
            else -> SEND_EXCHANGE_CONTACT
        }
    }
}
