package ru.grabovsky.dungeoncrusherbot.strategy.message

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.MazeDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerDto
import ru.grabovsky.poibot.dto.InlineMarkupDataDto

@Component
class MazeMessage(
    messageGenerateService: MessageGenerateService,
) : AbstractSendMessage<MazeDto>(messageGenerateService) {
    override fun inlineButtons(
        user: User,
        data: MazeDto?
    ): List<InlineMarkupDataDto> {
        return listOf(
            InlineMarkupDataDto(
                rowPos = 1,
                text = "↖\uFE0F",
                data = "LEFT"
            ),
            InlineMarkupDataDto(
                rowPos = 1,
                text = "⬆\uFE0F",
                data = "CENTER"
            ),
            InlineMarkupDataDto(
                rowPos = 1,
                text = "↗\uFE0F",
                data = "RIGHT"
            ),
            InlineMarkupDataDto(
                rowPos = 2,
                text = "\uD83E\uDDB6 Последние 20 шагов",
                data = "HISTORY"
            ),
        )
    }
}