package ru.grabovsky.dungeoncrusherbot.strategy.data.exchange

import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ExchangeRequestService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.data.AbstractDataRepository
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeRequestDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeResultDto
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Repository
class SearchExchangeResultDataRepository(
    private val stateService: StateService,
    private val exchangeRequestService: ExchangeRequestService,
) : AbstractDataRepository<ExchangeResultDto>() {

    override fun getData(user: TgUser): ExchangeResultDto {
        val state = stateService.getState(user)
        val data = state.callbackData?.split(" ") ?: emptyList()

        if (data.isEmpty()) throw IllegalStateException("Callback data not found for user ${user.userName}")

        val request = when (data[0]) {
            "SEARCH_EXCHANGE" -> exchangeRequestService.getRequestById(data[1].toLong())
            else -> throw IllegalStateException("Incorrect callback data for ${user.userName} for exchange request result")
        } ?: throw IllegalStateException("Not found exchange request with id ${data[1]}")

        return ExchangeResultDto(
            username = escapeMarkdown(request.user.userName),
            firstName = request.user.firstName!!,
            active = request.isActive,
            request = ExchangeRequestDto(
                pos = 1,
                id = request.id!!,
                type = request.type,
                sourcePrice = request.sourceResourcePrice,
                targetPrice = request.targetResourcePrice,
                targetServerId = request.targetServerId,
                sourceServerId = request.sourceServerId
            )
        )
    }

    private fun escapeMarkdown(source: String?): String? {
        source?: return null
        val regex = Regex("""([_*\[\]()~`>#+\-=|{}!])""")
        return source.replace(regex, """\\$1""")
    }
}