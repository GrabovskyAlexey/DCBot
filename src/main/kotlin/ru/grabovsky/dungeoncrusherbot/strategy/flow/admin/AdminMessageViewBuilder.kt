package ru.grabovsky.dungeoncrusherbot.strategy.flow.admin

import java.util.Locale
import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.AdminMessageDto
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowInlineButton
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowMessage
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowCallbackPayload
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cancelPromptButton
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.buildMessage

@Component
class AdminMessageViewBuilder(
    private val i18nService: I18nService,
) {
    private val key = FlowKeys.ADMIN_MESSAGE

    fun buildInboxMessage(
        dto: AdminMessageDto,
        messageId: Long,
        locale: Locale,
        includeActions: Boolean = true,
    ): FlowMessage {
        val buttons = if (includeActions) {
            listOf(
                FlowInlineButton(
                    text = i18nService.i18n(
                        "flow.admin.message.reply_button",
                        locale,
                        "✉️ Ответить"
                    ),
                    payload = FlowCallbackPayload(key.value, "REPLY:$messageId"),
                    row = 0,
                    col = 0
                ),
                FlowInlineButton(
                    text = i18nService.i18n(
                        "flow.admin.message.close_button",
                        locale,
                        "✅ Закрыть"
                    ),
                    payload = FlowCallbackPayload(key.value, "CLOSE:$messageId"),
                    row = 0,
                    col = 1
                )
            )
        } else {
            emptyList()
        }

        return key.buildMessage(
            step = AdminMessageStep.MAIN,
            model = dto,
            inlineButtons = buttons
        )
    }

    fun buildReplyPromptMessage(
        message: AdminPendingMessage,
        locale: Locale,
        invalid: Boolean
    ): FlowMessage = key.buildMessage(
        step = AdminMessageStep.PROMPT_REPLY,
        model = AdminReplyPromptModel(
            firstName = message.dto.firstName,
            userName = message.dto.userName,
            userId = message.dto.userId,
            invalid = invalid
        ),
        inlineButtons = listOf(
            key.cancelPromptButton(
                text = i18nService.i18n("flow.button.cancel", locale, "❌Отмена")
            )
        )
    )

    fun buildInfoMessage(text: String): FlowMessage =
        key.buildMessage(
            step = AdminMessageStep.INFO,
            model = AdminMessageInfoModel(text),
            inlineButtons = emptyList()
        )
}
