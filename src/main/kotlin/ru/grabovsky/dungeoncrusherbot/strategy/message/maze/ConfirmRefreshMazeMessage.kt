package ru.grabovsky.dungeoncrusherbot.strategy.message.maze

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.DataModel
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.util.Locale

@Component
class ConfirmRefreshMazeMessage(
    messageGenerateService: MessageGenerateService,
) : AbstractSendMessage<DataModel>(messageGenerateService) {
    override fun inlineButtons(user: User, data: DataModel?, locale: Locale): List<InlineMarkupDataDto> {
        return listOf(
            InlineMarkupDataDto(
                rowPos = 1,
                text = i18n(
                    code = "buttons.maze.confirm_refresh.confirm",
                    locale = locale,
                    default = "✅ДА"
                ),
                data = CallbackObject(
                    StateCode.CONFIRM_REFRESH_MAZE, "CONFIRM"
                ),
            ),
            InlineMarkupDataDto(
                rowPos = 1,
                text = i18n(
                    code = "buttons.maze.confirm_refresh.cancel",
                    locale = locale,
                    default = "❌НЕТ"
                ),
                data = CallbackObject(
                    StateCode.CONFIRM_REFRESH_MAZE, "NOT_CONFIRM"
                )
            ),
        )
    }
}
