package ru.grabovsky.dungeoncrusherbot.strategy.message.exchange

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeResultDto
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.util.*

@Component
class SearchExchangeResultMessage(messageGenerateService: MessageGenerateService): AbstractSendMessage<ExchangeResultDto>(messageGenerateService) {
    override fun inlineButtons(user: User, data: ExchangeResultDto?, locale: Locale): List<InlineMarkupDataDto> {
        return listOf(
            InlineMarkupDataDto(
                rowPos = 99,
                colPos = 1,
                text = i18n("buttons.exchange.back", locale, "\uD83D\uDD19 Назад"),
                data = CallbackObject(StateCode.SEARCH_EXCHANGE, "BACK")
            )
        )
    }
}