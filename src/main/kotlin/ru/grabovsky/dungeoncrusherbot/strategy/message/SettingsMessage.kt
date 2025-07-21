package ru.grabovsky.dungeoncrusherbot.strategy.message

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.SettingsDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

@Component
class SettingsMessage(
    messageGenerateService: MessageGenerateService,
) : AbstractSendMessage<SettingsDto>(messageGenerateService) {
    override fun inlineButtons(
        user: User,
        data: SettingsDto?
    ): List<InlineMarkupDataDto> {
        return listOf(
            InlineMarkupDataDto(
                rowPos = 1,
                text = getText(data, NotificationType.SIEGE),
                data = CallbackObject(StateCode.UPDATE_SETTINGS, "NOTIFY_SIEGE")
            ),
            InlineMarkupDataDto(
                rowPos = 2,
                text = getText(data, NotificationType.MINE),
                data = CallbackObject(StateCode.UPDATE_SETTINGS, "NOTIFY_MINE")
            ),
            InlineMarkupDataDto(
                rowPos = 3,
                text = if(data?.cbEnabled == true) "❌ Отключить учет КБ" else "✅ Включить учет КБ",
                data = CallbackObject(StateCode.UPDATE_SETTINGS, "CB_ENABLE")
            ),
            InlineMarkupDataDto(
                rowPos = 99,
                text = "✍\uFE0F Отправить пожелание\\сообщение об ошибке",
                data = CallbackObject(StateCode.UPDATE_SETTINGS, "SEND_REPORT")
            )
        )
    }


    private fun getText(dto: SettingsDto?, type: NotificationType): String {
        return when(type) {
            NotificationType.SIEGE -> {
                if (dto?.siegeEnabled == true) {
                    "\uD83D\uDCF4Включить в момент осады"
                } else {
                    "\uD83D\uDCF4Включить за 5 минут до осады"
                }
            }
            NotificationType.MINE -> {
                if (dto?.mineEnabled == true) {
                    "❌ Отключить КШ"
                } else {
                    "✅ Включить КШ"
                }
            }
        }
    }
}