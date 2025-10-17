package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.exchange

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.CallbackProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.ExecuteStatus
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Component
class ExchangeProcessor(
    stateService: StateService,
) : CallbackProcessor(stateService) {

    override fun process(user: TgUser, callbackData: String): ExecuteStatus {
        val data = callbackData.split(" ")
        if (data.isEmpty()) {
            return ExecuteStatus.NOTHING
        }

        val state = stateService.getState(user)
        state.callbackData = callbackData
        when (data[0]) {
            "EXCHANGE" -> state.lastServerIdByState[StateCode.EXCHANGE] = data[1].toInt()
            else -> return ExecuteStatus.NOTHING
        }
        stateService.saveState(state)
        return ExecuteStatus.FINAL
    }
}