package ru.grabovsky.dungeoncrusherbot.strategy.message.exchange

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeDetailDto
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.util.*

@Component
class ExchangeDetailMessage(
    messageGenerateService: MessageGenerateService
) : AbstractSendMessage<ExchangeDetailDto>(messageGenerateService) {
    override fun inlineButtons(user: User, data: ExchangeDetailDto?, locale: Locale): List<InlineMarkupDataDto> {
        data ?: return emptyList()

        val buttons =
            mutableListOf(
                InlineMarkupDataDto(
                    rowPos = 1,
                    colPos = 1,
                    text = i18n("buttons.exchange.request.create.exchange_map", locale, "\uD83E\uDE86 Обмен"),
                    data = CallbackObject(StateCode.EXCHANGE_DETAIL, "EXCHANGE_MAP")
                ),
                InlineMarkupDataDto(
                    rowPos = 1,
                    colPos = 2,
                    text = i18n("buttons.exchange.request.create.exchange_void", locale, "\uD83D\uDFE3 Обмен"),
                    data = CallbackObject(StateCode.EXCHANGE_DETAIL, "EXCHANGE_VOID")
                ),
                InlineMarkupDataDto(
                    rowPos = 2,
                    colPos = 1,
                    text = i18n("buttons.exchange.request.create.sell", locale, "\uD83E\uDE86 Продать"),
                    data = CallbackObject(StateCode.EXCHANGE_DETAIL, "SELL_MAP")
                ),
                InlineMarkupDataDto(
                    rowPos = 2,
                    colPos = 2,
                    text = i18n("buttons.exchange.request.create.buy", locale, "\uD83E\uDE86 Купить"),
                    data = CallbackObject(StateCode.EXCHANGE_DETAIL, "BUY_MAP")
                ),
                InlineMarkupDataDto(
                    rowPos = 97,
                    colPos = 1,
                    text = i18n("buttons.exchange.search", locale, "\uD83D\uDD0D Поиск"),
                    data = CallbackObject(StateCode.EXCHANGE_DETAIL, "SEARCH_EXCHANGE")
                ),
                InlineMarkupDataDto(
                    rowPos = 99,
                    colPos = 1,
                    text = i18n("buttons.exchange.back", locale, "\uD83D\uDD19 Назад"),
                    data = CallbackObject(StateCode.EXCHANGE_DETAIL, "BACK")
                )
            )
        if (data.requests.isNotEmpty()) {
            buttons.add(
                InlineMarkupDataDto(
                    rowPos = 98,
                    colPos = 1,
                    text = i18n("buttons.exchange.remove", locale, "❌ Удалить заявку"),
                    data = CallbackObject(StateCode.EXCHANGE_DETAIL, "REMOVE_EXCHANGE_REQUEST")
                )
            )
        }
        return buttons
    }
}