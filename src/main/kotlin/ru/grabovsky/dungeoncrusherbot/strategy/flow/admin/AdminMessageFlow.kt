package ru.grabovsky.dungeoncrusherbot.strategy.flow.admin

import java.util.Locale
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.AdminMessageDto
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.AnswerCallbackAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.EditMessageAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowCallbackContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowHandler
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowInlineButton
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKey
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowMessage
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowMessageContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowResult
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStartContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.SendMessageAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowCallbackPayload
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.buildMessage
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cancelPromptButton
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cleanupPromptMessages
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.finalizePrompt
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.retryPrompt
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.startPrompt

private const val PROMPT_MESSAGE_KEY = "admin_message_prompt"

@Component
class AdminMessageFlow(
    private val i18nService: I18nService,
    private val userService: UserService,
) : FlowHandler<AdminMessageFlowState> {

    override val key: FlowKey = FlowKeys.ADMIN_MESSAGE
    override val payloadType: Class<AdminMessageFlowState> = AdminMessageFlowState::class.java

    override fun start(context: FlowStartContext): FlowResult<AdminMessageFlowState> =
        FlowResult(
            stepKey = AdminMessageStep.MAIN.key,
            payload = AdminMessageFlowState(),
        )

    override fun onMessage(
        context: FlowMessageContext<AdminMessageFlowState>,
        message: Message
    ): FlowResult<AdminMessageFlowState>? {
        val text = message.text?.trim().orEmpty()
        val state = context.state.payload
        val pending = state.pendingReply ?: return null
        val targetMessage = state.messages.firstOrNull { it.id == pending.messageId }
            ?: return context.finalizePrompt(
                targetStep = AdminMessageStep.MAIN,
                userMessageId = message.messageId,
                updateState = { pendingReply = null }
            )

        if (text.isBlank()) {
            return context.retryPrompt(
                targetStep = AdminMessageStep.MAIN,
                bindingPrefix = PROMPT_MESSAGE_KEY,
                userMessageId = message.messageId,
                appendActions = { addAll(state.cleanupPromptMessages()) }
            ) {
                buildReplyPromptMessage(targetMessage, context.locale, invalid = true)
            }
        }

        userService.sendAdminReply(context.user, pending.dto.userId, text, pending.sourceMessageId)

        state.messages.removeIf { it.id == pending.messageId }

        val confirmation = i18nService.i18n(
            code = "flow.admin.message.reply_sent",
            locale = context.locale,
            default = "Ответ отправлен пользователю."
        )

        return context.finalizePrompt(
            targetStep = AdminMessageStep.MAIN,
            userMessageId = message.messageId,
            updateState = { pendingReply = null }
        ) {
            this += EditMessageAction(
                bindingKey = pending.bindingKey,
                message = buildInboxMessage(
                    dto = pending.dto,
                    messageId = pending.messageId,
                    locale = context.locale,
                    includeActions = false
                )
            )
            this += SendMessageAction(
                bindingKey = null,
                message = buildInfoMessage(confirmation)
            )
        }
    }

    override fun onCallback(
        context: FlowCallbackContext<AdminMessageFlowState>,
        callbackQuery: CallbackQuery,
        data: String
    ): FlowResult<AdminMessageFlowState>? {
        val (command, argument) = parseCallback(data)
        return when (command) {
            "REPLY" -> argument?.toLongOrNull()?.let { handleReply(context, callbackQuery, it) }
            "CLOSE" -> argument?.toLongOrNull()?.let { handleClose(context, callbackQuery, it) }
            else -> null
        }
    }

    private fun handleReply(
        context: FlowCallbackContext<AdminMessageFlowState>,
        callbackQuery: CallbackQuery,
        messageId: Long,
    ): FlowResult<AdminMessageFlowState> =
        context.withMessage(messageId) { state, message ->
            val cleanup = state.cleanupPromptMessages()
            context.startPrompt(
                targetStep = AdminMessageStep.MAIN,
                bindingPrefix = PROMPT_MESSAGE_KEY,
                callbackQuery = callbackQuery,
                updateState = {
                    pendingReply = AdminPendingReply(
                        messageId = message.id,
                        bindingKey = message.bindingKey,
                        dto = message.dto,
                        sourceMessageId = message.sourceMessageId
                    )
                },
                appendActions = { addAll(cleanup) }
            ) {
                buildReplyPromptMessage(message, context.locale, invalid = false)
            }
        } ?: context.notFoundResult(callbackQuery)

    private fun handleClose(
        context: FlowCallbackContext<AdminMessageFlowState>,
        callbackQuery: CallbackQuery,
        messageId: Long,
    ): FlowResult<AdminMessageFlowState> =
        context.withMessage(messageId) { state, message ->
            val actions = state.cleanupPromptMessages()
            if (state.pendingReply?.messageId == messageId) {
                state.pendingReply = null
            }
            state.messages.removeIf { it.id == messageId }

            val infoText = i18nService.i18n(
                code = "flow.admin.message.closed",
                locale = context.locale,
                default = "Сообщение закрыто."
            )

            actions += EditMessageAction(
                bindingKey = message.bindingKey,
                message = buildInboxMessage(
                    dto = message.dto,
                    messageId = message.id,
                    locale = context.locale,
                    includeActions = false
                )
            )
            actions += SendMessageAction(
                bindingKey = null,
                message = buildInfoMessage(infoText)
            )
            actions += AnswerCallbackAction(callbackQuery.id)

            FlowResult(
                stepKey = AdminMessageStep.MAIN.key,
                payload = state,
                actions = actions
            )
        } ?: context.notFoundResult(callbackQuery)

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

    private fun buildReplyPromptMessage(
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

    private fun buildInfoMessage(text: String): FlowMessage =
        key.buildMessage(
            step = AdminMessageStep.INFO,
            model = AdminMessageInfoModel(text),
            inlineButtons = emptyList()
        )

    private fun parseCallback(data: String): Pair<String, String?> =
        if (data.contains(':')) {
            val parts = data.split(':', limit = 2)
            parts[0] to parts[1]
        } else {
            data to null
        }

    private fun FlowCallbackContext<AdminMessageFlowState>.withMessage(
        messageId: Long,
        block: (AdminMessageFlowState, AdminPendingMessage) -> FlowResult<AdminMessageFlowState>?
    ): FlowResult<AdminMessageFlowState>? {
        val flowState = state.payload
        val message = flowState.messages.firstOrNull { it.id == messageId } ?: return null
        return block(flowState, message)
    }

    private fun FlowCallbackContext<AdminMessageFlowState>.notFoundResult(
        callbackQuery: CallbackQuery
    ): FlowResult<AdminMessageFlowState> =
        FlowResult(
            stepKey = state.stepKey,
            payload = state.payload,
            actions = listOf(
                AnswerCallbackAction(
                    callbackQueryId = callbackQuery.id,
                    text = i18nService.i18n(
                        "flow.admin.message.not_found",
                        locale,
                        "Сообщение уже обработано"
                    )
                )
            )
        )
}

data class AdminMessageInfoModel(val text: String)

data class AdminReplyPromptModel(
    val firstName: String,
    val userName: String?,
    val userId: Long,
    val invalid: Boolean,
)
