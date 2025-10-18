package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.exchange

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.CallbackProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.ExecuteStatus

@Component
class SearchExchangeProcessor(
    stateService: StateService,
) : CallbackProcessor(stateService) {
    override fun process(
        user: User,
        callbackData: String
    ): ExecuteStatus {
        val data = callbackData.split(" ")
        if (data.isEmpty()) {
            return ExecuteStatus.NOTHING
        }
        val state = stateService.getState(user)
        state.callbackData = callbackData
        return when (data[0]) {
            "BACK" -> ExecuteStatus.FINAL
            "SEARCH_EXCHANGE" -> ExecuteStatus.FINAL
            else -> ExecuteStatus.NOTHING
        }
    }
}