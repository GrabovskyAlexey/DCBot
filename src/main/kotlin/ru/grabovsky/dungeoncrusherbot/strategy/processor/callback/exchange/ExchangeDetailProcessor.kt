package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.exchange

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.CallbackData
import ru.grabovsky.dungeoncrusherbot.entity.CallbackDataType.*
import ru.grabovsky.dungeoncrusherbot.entity.CallbackExchangeRequest
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequestType.*
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeResourceType.*
import ru.grabovsky.dungeoncrusherbot.repository.CallbackDataRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.CallbackProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.ExecuteStatus
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.lang.IllegalStateException

@Component
class ExchangeDetailProcessor(
    stateService: StateService,
    private val callbackDataRepository: CallbackDataRepository,
    private val objectMapper: ObjectMapper
) : CallbackProcessor(stateService) {
    override fun process(
        user: User,
        callbackData: String
    ): ExecuteStatus {
        val request = callbackDataRepository.findByTypeAndUserId(EXCHANGE, user.id) ?: CallbackData(
            userId = user.id,
            type = EXCHANGE)
        val state = stateService.getState(user)
        val lastServerId = state.lastServerIdByState[StateCode.EXCHANGE] ?: throw IllegalStateException("Last server id for ${user.id} and state=EXCHANGE is unknown")
        val data = when (callbackData) {
            "EXCHANGE_MAP" -> CallbackExchangeRequest(
                type = EXCHANGE_MAP,
                sourceServerId = lastServerId,
                sourceResourceType = MAP,
                targetResourceType = MAP,
                sourceResourcePrice = 1,
                targetResourcePrice = 1
            )
            "EXCHANGE_VOID" -> CallbackExchangeRequest(
                type = EXCHANGE_VOID,
                sourceServerId = lastServerId,
                sourceResourceType = VOID,
                targetResourceType = VOID,
                sourceResourcePrice = 1,
                targetResourcePrice = 1
            )
            "SELL_MAP" -> CallbackExchangeRequest(
                type = SELL_MAP,
                sourceServerId = lastServerId,
                targetServerId = lastServerId,
                sourceResourceType = MAP,
                targetResourceType = VOID
            )
            "BUY_MAP" -> CallbackExchangeRequest(
                type = BUY_MAP,
                sourceServerId = lastServerId,
                targetServerId = lastServerId,
                sourceResourceType = VOID,
                targetResourceType = MAP
            )
            else -> null
        }
        request.data = objectMapper.writeValueAsString(data)
        request.also { callbackDataRepository.save(it) }
        return ExecuteStatus.FINAL
    }
}
