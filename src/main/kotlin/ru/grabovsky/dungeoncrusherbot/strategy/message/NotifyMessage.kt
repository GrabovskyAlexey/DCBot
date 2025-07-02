package ru.grabovsky.dungeoncrusherbot.strategy.message

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.MazeDto
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.entity.NotificationSubscribe
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.strategy.dto.NotifyDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

@Component
class NotifyMessage(
    messageGenerateService: MessageGenerateService,
) : AbstractSendMessage<NotifyDto>(messageGenerateService) {
    override fun inlineButtons(
        user: User,
        data: NotifyDto?
    ): List<InlineMarkupDataDto> {
        return listOf(
            InlineMarkupDataDto(
                rowPos = 1,
                text = getButtonText(data?.notifications?.firstOrNull { it.type == NotificationType.SIEGE }, " 5 мин. до осады"),
                data = CallbackObject(StateCode.UPDATE_NOTIFY, "NOTIFY_SIEGE")
            ),
            InlineMarkupDataDto(
                rowPos = 2,
                text = getButtonText(data?.notifications?.firstOrNull { it.type == NotificationType.MINE }, "КШ"),
                data = CallbackObject(StateCode.UPDATE_NOTIFY, "NOTIFY_MINE")
            )
        )
    }

    private fun getButtonText(data: NotificationSubscribe?, postfix: String): String {
        return if (data?.enabled == true) {
            "❌ Отключить $postfix"
        } else {
            "✅ Включить $postfix"
        }
    }
}