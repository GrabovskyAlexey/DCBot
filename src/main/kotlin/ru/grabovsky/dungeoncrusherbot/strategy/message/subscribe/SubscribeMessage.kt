package ru.grabovsky.dungeoncrusherbot.strategy.message.subscribe

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerDto
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.util.Locale

@Component
class SubscribeMessage(
    messageGenerateService: MessageGenerateService,
    private val userService: UserService,
    private val serverService: ServerService
) :
    AbstractSendMessage<ServerDto>(messageGenerateService) {
    override fun inlineButtons(user: User, data: ServerDto?, locale: Locale): List<InlineMarkupDataDto> {
        val servers = userService.getUser(user.id)?.servers ?: emptySet()
        val allServers = serverService.getAllServers()
        val result: MutableList<InlineMarkupDataDto> = mutableListOf()
        var row = 1
        var count = 0
        for (server in allServers) {
            val isSubscribed = servers.any { it == server }
            val markUp = InlineMarkupDataDto(
                rowPos = row,
                text = if (isSubscribed) "✅ ${server.id}" else "❌ ${server.id}",
                data = CallbackObject(StateCode.SUBSCRIBE, if (isSubscribed) "UNSUBSCRIBE ${server.id}" else "SUBSCRIBE ${server.id}")
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