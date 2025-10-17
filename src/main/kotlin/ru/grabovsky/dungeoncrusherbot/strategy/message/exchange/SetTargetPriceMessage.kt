package ru.grabovsky.dungeoncrusherbot.strategy.message.exchange

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.DataModel
import ru.grabovsky.dungeoncrusherbot.strategy.dto.PriceDto
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.util.Locale

@Component
class SetTargetPriceMessage(messageGenerateService: MessageGenerateService): AbstractSendMessage<PriceDto>(messageGenerateService) {
    override fun inlineButtons(user: User, data: PriceDto?, locale: Locale): List<InlineMarkupDataDto> {
        var row = 1
        var column = 1
        return buildList {
            (1..10).forEach { id ->
                add(
                    InlineMarkupDataDto(
                        rowPos = row,
                        colPos = column,
                        text = id.toString(),
                        data = CallbackObject(StateCode.SET_TARGET_PRICE, "SET_TARGET_PRICE $id")
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
                data = CallbackObject(StateCode.SET_TARGET_PRICE, "BACK")
            ))
        }
    }
}