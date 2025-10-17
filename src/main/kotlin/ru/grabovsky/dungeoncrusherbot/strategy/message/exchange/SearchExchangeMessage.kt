package ru.grabovsky.dungeoncrusherbot.strategy.message.exchange

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.DataModel
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeDetailDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.PriceDto
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.util.Locale

@Component
class SearchExchangeMessage(messageGenerateService: MessageGenerateService): AbstractSendMessage<ExchangeDetailDto>(messageGenerateService) {
    override fun inlineButtons(user: User, data: ExchangeDetailDto?, locale: Locale): List<InlineMarkupDataDto> {
        data ?: return emptyList()
        var row = 1
        var column = 1
        return buildList {
            data.requests.forEach { request ->
                add(
                    InlineMarkupDataDto(
                        rowPos = row,
                        colPos = column,
                        text = "${request.pos}",
                        data = CallbackObject(StateCode.SEARCH_EXCHANGE, "SEARCH_EXCHANGE ${request.id}")
                    )
                )
                column++
                if (column > 5) {
                    column = 1
                    row++
                }
            }
            add(InlineMarkupDataDto(
                rowPos = 99,
                colPos = 1,
                text = i18n("buttons.exchange.back", locale, "\uD83D\uDD19 Назад"),
                data = CallbackObject(StateCode.SEARCH_EXCHANGE, "BACK")
            ))
        }
    }
}