package ru.grabovsky.dungeoncrusherbot.strategy.message

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ResourceDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

@Component
class ResourcesMessage(
    messageGenerateService: MessageGenerateService,
    private val serverService: ServerService
) :
    AbstractSendMessage<ResourceDto>(messageGenerateService) {
    override fun inlineButtons(
        user: User,
        data: ResourceDto?
    ): List<InlineMarkupDataDto> {
        val allServers = serverService.getAllServers()
        val mainServerId = data?.servers?.firstOrNull { it.main }?.id
        val result: MutableList<InlineMarkupDataDto> = mutableListOf()
        var row = 1
        var count = 0
        for (server in allServers) {
            val markUp = InlineMarkupDataDto(
                rowPos = row,
                text = "${if(server.id == mainServerId) "\uD83D\uDC51" else ""}${server.id}",
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