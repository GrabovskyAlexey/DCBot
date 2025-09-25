package ru.grabovsky.dungeoncrusherbot.strategy.message.settings

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.SettingsDto
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.util.Locale

@Component
class SettingsMessage(
    messageGenerateService: MessageGenerateService,
) : AbstractSendMessage<SettingsDto>(messageGenerateService) {
    override fun inlineButtons(user: User, data: SettingsDto?, locale: Locale): List<InlineMarkupDataDto> {
        val buttons = mutableListOf(
            InlineMarkupDataDto(
                rowPos = 1,
                text = getText(data, NotificationType.SIEGE, locale),
                data = CallbackObject(StateCode.UPDATE_SETTINGS, "NOTIFY_SIEGE"),
            ),
            InlineMarkupDataDto(
                rowPos = 2,
                text = getText(data, NotificationType.MINE, locale),
                data = CallbackObject(StateCode.UPDATE_SETTINGS, "NOTIFY_MINE"),
            ),
            InlineMarkupDataDto(
                rowPos = 3,
                text = i18n(
                    code = if (data?.cbEnabled == true) "buttons.settings.cb.disable" else "buttons.settings.cb.enable",
                    locale = locale,
                    default = if (data?.cbEnabled == true) "❌ Отключить учет КБ" else "✅ Включить учет КБ"
                ),
                data = CallbackObject(StateCode.UPDATE_SETTINGS, "CB_ENABLE"),
            ),
        )

        buttons += InlineMarkupDataDto(
            rowPos = 4,
            text = i18n(
                code = if (data?.quickResourceEnabled == true) "buttons.settings.quick.disable" else "buttons.settings.quick.enable",
                locale = locale,
                default = if (data?.quickResourceEnabled == true) "❌ Отключить быстрый учет" else "✅ Включить быстрый учет"
            ),
            data = CallbackObject(StateCode.UPDATE_SETTINGS, "QUICK_RESOURCES"),
        )

        buttons += InlineMarkupDataDto(
            rowPos = 99,
            text = i18n(
                code = "buttons.settings.send_report",
                locale = locale,
                default = "✍\uFE0F Отправить пожелание\\сообщение об ошибке"
            ),
            data = CallbackObject(StateCode.UPDATE_SETTINGS, "SEND_REPORT"),
        )

        return buttons
    }

    private fun getText(dto: SettingsDto?, type: NotificationType, locale: Locale): String {
        return when (type) {
            NotificationType.SIEGE -> {
                if (dto?.siegeEnabled == true) {
                    i18n(
                        code = "buttons.settings.siege.enabled",
                        locale = locale,
                        default = "\uD83D\uDCF4Включить в момент осады"
                    )
                } else {
                    i18n(
                        code = "buttons.settings.siege.disabled",
                        locale = locale,
                        default = "\uD83D\uDCF4Включить за 5 минут до осады"
                    )
                }
            }
            NotificationType.MINE -> {
                if (dto?.mineEnabled == true) {
                    i18n(
                        code = "buttons.settings.mine.disable",
                        locale = locale,
                        default = "❌ Отключить КШ"
                    )
                } else {
                    i18n(
                        code = "buttons.settings.mine.enable",
                        locale = locale,
                        default = "✅ Включить КШ"
                    )
                }
            }
        }
    }
}
