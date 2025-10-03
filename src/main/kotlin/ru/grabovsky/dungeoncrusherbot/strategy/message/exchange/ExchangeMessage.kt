package ru.grabovsky.dungeoncrusherbot.strategy.message.exchange

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeDto
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.util.Locale

@Component
open class ExchangeMessage(
    messageGenerateService: MessageGenerateService
) : AbstractSendMessage<ExchangeDto>(messageGenerateService) {

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
                        data = CallbackObject(StateCode.EXCHANGE, "SERVER ${server.id}")
                    )
                )
                column++
                if (column > 5) {
                    column = 1
                    row++
                }
            }
        }
    }

    private fun buildServerLabel(server: ExchangeDto.Server): String {
        val prefix = if (server.hasExchange && !server.main) "✅" else ""
        val suffix = if (server.main) "👑" else ""
        return "$prefix${server.id}$suffix"
    }
}

