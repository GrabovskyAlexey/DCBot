package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.exchange

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.CallbackProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.ExecuteStatus

@Component
class SearchExchangeResultProcessor(
    stateService: StateService,
) : CallbackProcessor(stateService) {
    override fun process(
        user: User,
        callbackData: String
    ): ExecuteStatus {
        val state = stateService.getState(user)
        state.callbackData = callbackData
        return when (callbackData) {
            "BACK" -> ExecuteStatus.FINAL
            else -> ExecuteStatus.NOTHING
        }
    }
}