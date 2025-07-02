package ru.grabovsky.dungeoncrusherbot.strategy.message

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.MazeDto
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

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
                data = CallbackObject(
                    StateCode.UPDATE_MAZE,"LEFT")
            ),
            InlineMarkupDataDto(
                rowPos = 1,
                text = "⬆\uFE0F",
                data = CallbackObject(
                    StateCode.UPDATE_MAZE,"CENTER")
            ),
            InlineMarkupDataDto(
                rowPos = 1,
                text = "↗\uFE0F",
                data = CallbackObject(
                    StateCode.UPDATE_MAZE,"RIGHT")
            ),
            InlineMarkupDataDto(
                rowPos = 2,
                text = "\uD83E\uDDB6 Последние 20 шагов",
                data = CallbackObject(
                    StateCode.UPDATE_MAZE,"HISTORY")
            ),
            InlineMarkupDataDto(
                rowPos = 3,
                text = "\uD83D\uDDD1 Сбросить прогресс",
                data = CallbackObject(
                    StateCode.UPDATE_MAZE,"REFRESH_MAZE")
            ),
        )
    }
}