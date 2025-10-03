package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Component
class ExchangeProcessor(
    stateService: StateService
) : CallbackProcessor(stateService) {
    override fun process(user: TgUser, callbackData: String): ExecuteStatus {
        val parts = callbackData.split(" ")
        if (parts.isEmpty()) {
            return ExecuteStatus.NOTHING
        }

        val state = stateService.getState(user)
        when (parts[0]) {
            "SERVER" -> {
                val serverId = parts.getOrNull(1)?.toIntOrNull() ?: return ExecuteStatus.NOTHING
                val nextState = if (state.state == StateCode.EXCHANGE_DETAIL || state.state == StateCode.UPDATE_EXCHANGE_DETAIL) {
                    StateCode.UPDATE_EXCHANGE_DETAIL
                } else {
                    StateCode.EXCHANGE_DETAIL
                }
                state.state = nextState
                state.prevState = StateCode.EXCHANGE
                state.callbackData = "DETAIL:$serverId"
                stateService.saveState(state)
            }
            "BACK" -> {
                state.state = StateCode.UPDATE_EXCHANGE
                state.prevState = StateCode.EXCHANGE_DETAIL
                state.callbackData = null
                stateService.saveState(state)
            }
            else -> return ExecuteStatus.NOTHING
        }
        return ExecuteStatus.FINAL
    }
}