package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.exchange

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.CallbackDataType
import ru.grabovsky.dungeoncrusherbot.entity.CallbackExchangeRequest
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ExchangeRequestService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.CallbackProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.ExecuteStatus

@Component
class RemoveExchangeRequestProcessor(
    stateService: StateService,
    private val exchangeRequestService: ExchangeRequestService
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
        val requestId = when (data[0]) {
            "REMOVE_REQUEST" -> data[1].toLong()
            "BACK" -> return ExecuteStatus.FINAL
            else -> return ExecuteStatus.NOTHING
        }
        exchangeRequestService.setRequestInactiveById(requestId)
        return ExecuteStatus.FINAL
    }
}