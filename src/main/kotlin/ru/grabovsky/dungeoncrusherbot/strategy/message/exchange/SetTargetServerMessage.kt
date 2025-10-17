package ru.grabovsky.dungeoncrusherbot.strategy.message.exchange

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.PriceDto
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.util.Locale

@Component
class SetTargetServerMessage(
    messageGenerateService: MessageGenerateService,
): AbstractSendMessage<ExchangeDto>(messageGenerateService) {
    override fun inlineButtons(user: User, data: ExchangeDto?, locale: Locale): List<InlineMarkupDataDto> {
        val dto = data ?: return emptyList()
        if (!dto.hasServers) return emptyList()

        var row = 1
        var column = 1
        return buildList {
            dto.servers.forEach { server ->
                add(
                    InlineMarkupDataDto(
                        rowPos = row,
                        colPos = column,
                        text = buildServerLabel(server),
                        data = CallbackObject(StateCode.SET_TARGET_SERVER, "SET_TARGET_SERVER ${server.id}")
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
                data = CallbackObject(StateCode.SET_TARGET_SERVER, "BACK")
            ))
        }
    }

    private fun buildServerLabel(server: ExchangeDto.Server): String {
        val suffix = if (server.main) "👑" else ""
        return "${server.id}$suffix"
    }
}