package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.exchange

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.CallbackDataType
import ru.grabovsky.dungeoncrusherbot.entity.CallbackExchangeRequest
import ru.grabovsky.dungeoncrusherbot.repository.CallbackDataRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ExchangeRequestService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.CallbackProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.ExecuteStatus

@Component
class SetTargetPriceProcessor(
    stateService: StateService,
    private val exchangeRequestService: ExchangeRequestService,
    private val callbackDataRepository: CallbackDataRepository,
    private val userService: UserService,
    private val objectMapper: ObjectMapper
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
        val price = when (data[0]) {
            "SET_TARGET_PRICE" -> data[1].toInt()
            "BACK" -> return ExecuteStatus.FINAL
            else -> return ExecuteStatus.NOTHING
        }
        val request = callbackDataRepository.findByTypeAndUserId(CallbackDataType.EXCHANGE, user.id)
        val tempRequest = request?.let { objectMapper.readValue(it.data,
            CallbackExchangeRequest::class.java) } ?: error("No callback exchange request found for ${user.id} and type: EXCHANGE")

        val userFromDb = userService.getUser(user.id) ?: error("User ${user.id} not found")
        exchangeRequestService.createOrUpdateExchangeRequest(
            user = userFromDb,
            exchangeRequestType = tempRequest.type,
            sourceServerId = tempRequest.sourceServerId,
            targetServerId = tempRequest.targetServerId,
            sourceResourceType = tempRequest.sourceResourceType,
            targetResourceType = tempRequest.targetResourceType,
            sourcePrice = tempRequest.sourceResourcePrice?: 1,
            targetPrice = price
        )
        callbackDataRepository.delete(request)
        return ExecuteStatus.FINAL
    }
}