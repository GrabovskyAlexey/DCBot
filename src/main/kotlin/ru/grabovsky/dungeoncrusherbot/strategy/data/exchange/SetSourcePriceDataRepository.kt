package ru.grabovsky.dungeoncrusherbot.strategy.data.exchange

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.entity.CallbackDataType
import ru.grabovsky.dungeoncrusherbot.entity.CallbackExchangeRequest
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequestType
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequestType.*
import ru.grabovsky.dungeoncrusherbot.repository.CallbackDataRepository
import org.telegram.telegrambots.meta.api.objects.User as TgUser
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.data.AbstractDataRepository
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.PriceDto

@Repository
class SetSourcePriceDataRepository(
    private val objectMapper: ObjectMapper,
    private val callbackDataRepository: CallbackDataRepository
) : AbstractDataRepository<PriceDto>() {

    override fun getData(user: TgUser): PriceDto {
        val data = callbackDataRepository.findByTypeAndUserId(CallbackDataType.EXCHANGE, user.id)?.data
            ?.let { objectMapper.readValue(it, CallbackExchangeRequest::class.java) }
            ?: throw IllegalStateException("Callback exchange request not found for user ${user.userName ?: user.firstName}")

        val resource = when {
            user.languageCode == "ru" && data.type == SELL_MAP -> "карт"
            user.languageCode == "ru" && data.type == BUY_MAP -> "пустот"
            data.type == SELL_MAP -> "maps"
            data.type == BUY_MAP -> "voids"
            else -> ""
        }
        return PriceDto(
            resource = resource
        )
    }
}
