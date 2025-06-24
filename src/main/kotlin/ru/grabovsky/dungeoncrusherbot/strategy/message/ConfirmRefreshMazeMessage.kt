package ru.grabovsky.dungeoncrusherbot.strategy.message

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.DataModel
import ru.grabovsky.dungeoncrusherbot.strategy.dto.MazeDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerDto
import ru.grabovsky.poibot.dto.InlineMarkupDataDto

@Component
class ConfirmRefreshMazeMessage(
    messageGenerateService: MessageGenerateService,
) : AbstractSendMessage<DataModel>(messageGenerateService) {
    override fun inlineButtons(
        user: User,
        data: DataModel?
    ): List<InlineMarkupDataDto> {
        return listOf(
            InlineMarkupDataDto(
                rowPos = 1,
                text = "✅ДА",
                data = "REFRESH_MAZE_CONFIRM"
            ),
            InlineMarkupDataDto(
                rowPos = 1,
                text = "❌НЕТ",
                data = "REFRESH_MAZE_NOT_CONFIRM"
            ),
        )
    }
}