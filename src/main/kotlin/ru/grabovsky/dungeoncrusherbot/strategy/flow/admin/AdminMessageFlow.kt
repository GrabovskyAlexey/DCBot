package ru.grabovsky.dungeoncrusherbot.strategy.flow.admin

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.AnswerCallbackAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.EditMessageAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowCallbackContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowHandler
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKey
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowMessageContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowResult
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStartContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.SendMessageAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.SetReactionAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cleanupPromptMessages
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.finalizePrompt
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.retryPrompt
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.startPrompt

private const val PROMPT_MESSAGE_KEY = "admin_message_prompt"

@Component
class AdminMessageFlow(
    private val i18nService: I18nService,
    private val userService: UserService,
    private val viewBuilder: AdminMessageViewBuilder,
) : FlowHandler<AdminMessageFlowState> {

    override val key: FlowKey = FlowKeys.ADMIN_MESSAGE
    override val payloadType: Class<AdminMessageFlowState> = AdminMessageFlowState::class.java

    override fun start(context: FlowStartContext): FlowResult<AdminMessageFlowState> =
        FlowResult(
            stepKey = AdminMessageStep.MAIN.key,
            payload = AdminMessageFlowState(),
        )

    override fun onMessage(
        context: FlowContext<AdminMessageFlowState>,
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
                viewBuilder.buildReplyPromptMessage(targetMessage, context.locale, invalid = true)
            }
        }

        userService.sendAdminReply(context.user, pending.dto.userId, text, pending.sourceMessageId)

        state.messages.removeIf { it.id == pending.messageId }

        return context.finalizePrompt(
            targetStep = AdminMessageStep.MAIN,
            userMessageId = null,
            updateState = { pendingReply = null }
        ) {
            this += EditMessageAction(
                bindingKey = pending.bindingKey,
                message = viewBuilder.buildInboxMessage(
                    dto = pending.dto,
                    messageId = pending.messageId,
                    locale = context.locale,
                    includeActions = false
                )
            )
            this += SetReactionAction(
                chatId = message.chatId,
                messageId = message.messageId,
                emoji = "\uD83D\uDC4D"
            )
        }
    }

    override fun onCallback(
        context: FlowContext<AdminMessageFlowState>,
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
        context: FlowContext<AdminMessageFlowState>,
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
                viewBuilder.buildReplyPromptMessage(message, context.locale, invalid = false)
            }
        } ?: context.notFoundResult(callbackQuery)

    private fun handleClose(
        context: FlowContext<AdminMessageFlowState>,
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
                message = viewBuilder.buildInboxMessage(
                    dto = message.dto,
                    messageId = message.id,
                    locale = context.locale,
                    includeActions = false
                )
            )
            actions += SendMessageAction(
                bindingKey = null,
                message = viewBuilder.buildInfoMessage(infoText)
            )
            actions += AnswerCallbackAction(callbackQuery.id)

            FlowResult(
                stepKey = AdminMessageStep.MAIN.key,
                payload = state,
                actions = actions
            )
        } ?: context.notFoundResult(callbackQuery)

    private fun parseCallback(data: String): Pair<String, String?> =
        if (data.contains(':')) {
            val parts = data.split(':', limit = 2)
            parts[0] to parts[1]
        } else {
            data to null
        }

    private fun FlowContext<AdminMessageFlowState>.withMessage(
        messageId: Long,
        block: (AdminMessageFlowState, AdminPendingMessage) -> FlowResult<AdminMessageFlowState>?
    ): FlowResult<AdminMessageFlowState>? {
        val flowState = state.payload
        val message = flowState.messages.firstOrNull { it.id == messageId } ?: return null
        return block(flowState, message)
    }

    private fun FlowContext<AdminMessageFlowState>.notFoundResult(
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
