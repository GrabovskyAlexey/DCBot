package ru.grabovsky.dungeoncrusherbot.strategy.message.resources

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ResourceDto
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.util.Locale

@Component
class ResourcesMessage(
    messageGenerateService: MessageGenerateService,
    private val serverService: ServerService
) :
    AbstractSendMessage<ResourceDto>(messageGenerateService) {
    override fun inlineButtons(
        user: User,
        data: ResourceDto?,
        locale: Locale
    ): List<InlineMarkupDataDto> {
        val allServers = serverService.getAllServers()
        val mainServerId = data?.servers?.firstOrNull { it.main }?.id
        val result: MutableList<InlineMarkupDataDto> = mutableListOf()
        var row = 1
        var count = 0
        for (server in allServers) {
            val isMainServer = server.id == mainServerId
            val code = if (isMainServer) "buttons.resources.server.main" else "buttons.resources.server.regular"
            val default = if (isMainServer) "\uD83D\uDC51{0}" else "{0}"
            val markUp = InlineMarkupDataDto(
                rowPos = row,
                text = i18n(code, locale, default, server.id),
                data = CallbackObject(StateCode.RESOURCES, "RESOURCE ${server.id}")
            )
            count++
            if(count >= 5) {
                row++
                count = 0
            }
            result.add(markUp)
        }
        return result
    }
}
