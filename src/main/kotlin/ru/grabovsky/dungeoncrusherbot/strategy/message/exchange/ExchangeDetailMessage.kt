package ru.grabovsky.dungeoncrusherbot.strategy.message.exchange

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeDetailDto
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.util.Locale

abstract class AbstractExchangeDetailMessage(
    messageGenerateService: MessageGenerateService
) : AbstractSendMessage<ExchangeDetailDto>(messageGenerateService) {

    override fun inlineButtons(user: User, data: ExchangeDetailDto?, locale: Locale): List<InlineMarkupDataDto> =
        listOf(
            InlineMarkupDataDto(
                rowPos = 99,
                colPos = 1,
                text = i18n("buttons.exchange.back", locale, "↩️ Back"),
                data = CallbackObject(StateCode.EXCHANGE, "BACK")
            )
        )
}

@Component
class ExchangeDetailMessage(
    messageGenerateService: MessageGenerateService
) : AbstractExchangeDetailMessage(messageGenerateService)

@Component
class UpdateExchangeDetailMessage(
    messageGenerateService: MessageGenerateService
) : AbstractExchangeDetailMessage(messageGenerateService)