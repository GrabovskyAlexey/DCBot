package ru.grabovsky.dungeoncrusherbot.strategy.flow.core.telegram

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.AnswerCallbackAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.DeleteMessageAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.EditMessageAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowActionExecutor
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowBindingsMutation
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowInlineButton
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowMessage
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.SendMessageAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.DeleteMessageIdAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.templating.FlowTemplateRenderer
import java.util.Locale

@Component
class TelegramFlowActionExecutor(
    private val telegramClient: TelegramClient,
    private val objectMapper: ObjectMapper,
    private val templateRenderer: FlowTemplateRenderer,
) : FlowActionExecutor {
    override fun execute(
        user: User,
        locale: Locale,
        currentBindings: Map<String, Int>,
        actions: List<FlowAction>,
    ): FlowBindingsMutation {
        if (actions.isEmpty()) {
            return FlowBindingsMutation()
        }
        val replacements = mutableMapOf<String, Int>()
        val removed = mutableSetOf<String>()

        actions.forEach { action ->
            when (action) {
                is SendMessageAction -> {
                    val rendered = renderMessage(action.message, locale)
                    val result = telegramClient.execute(
                        buildSendMessage(user.id, rendered, action.message)
                    )
                    action.bindingKey?.let { replacements[it] = result.messageId }
                }

                is EditMessageAction -> {
                    val messageId = currentBindings[action.bindingKey]
                        ?: error("Message binding ${action.bindingKey} not found for user ${user.id}")
                    val rendered = renderMessage(action.message, locale)
                    telegramClient.execute(
                        buildEditMessage(user.id, messageId, rendered, action.message)
                    )
                }

                is DeleteMessageAction -> {
                    val messageId = currentBindings[action.bindingKey]
                        ?: return@forEach
                    telegramClient.execute(
                        DeleteMessages.builder()
                            .chatId(user.id)
                            .messageIds(listOf(messageId))
                            .build()
                    )
                    removed += action.bindingKey
                }

                is DeleteMessageIdAction -> telegramClient.execute(
                    DeleteMessages.builder()
                        .chatId(user.id)
                        .messageIds(listOf(action.messageId))
                        .build()
                )

                is AnswerCallbackAction -> telegramClient.execute(
                    AnswerCallbackQuery.builder()
                        .callbackQueryId(action.callbackQueryId)
                        .text(action.text)
                        .showAlert(action.showAlert)
                        .build()
                )
            }
        }

        return FlowBindingsMutation(
            replacements = replacements,
            removed = removed,
        )
    }

    private fun renderMessage(message: FlowMessage, locale: Locale): String =
        templateRenderer.render(message.flowKey, message.stepKey, locale, message.model)

    private fun buildSendMessage(chatId: Long, text: String, message: FlowMessage): SendMessage {
        val sendMessage = SendMessage.builder()
            .chatId(chatId)
            .text(text)
            .build()

        message.parseMode.telegramValue?.let { sendMessage.parseMode = it }
        sendMessage.replyMarkup = buildReplyMarkup(message)
        return sendMessage
    }

    private fun buildEditMessage(chatId: Long, messageId: Int, text: String, message: FlowMessage): EditMessageText {
        val editMessage = EditMessageText.builder()
            .chatId(chatId)
            .messageId(messageId)
            .text(text)
            .build()

        message.parseMode.telegramValue?.let { editMessage.parseMode = it }
        editMessage.replyMarkup = buildInlineMarkup(message.inlineButtons)
        return editMessage
    }

    private fun buildReplyMarkup(message: FlowMessage): ReplyKeyboard? {
        return message.inlineButtons.takeIf { it.isNotEmpty() }?.let { buildInlineMarkup(it) }
            ?: message.replyButtons.takeIf { it.isNotEmpty() }?.let { replyButtons ->
                val keyboardRows = replyButtons.map { button ->
                    KeyboardRow().apply {
                        add(
                            KeyboardButton.builder()
                                .text(button.text)
                                .requestLocation(button.requestLocation)
                                .build()
                        )
                    }
                }
                ReplyKeyboardMarkup.builder()
                    .keyboard(keyboardRows)
                    .resizeKeyboard(true)
                    .build()
            }
    }

    private fun buildInlineMarkup(buttons: List<FlowInlineButton>): InlineKeyboardMarkup? {
        if (buttons.isEmpty()) {
            return null
        }
        val grouped = buttons.groupBy { it.row }.toSortedMap()
        val rows = grouped.map { (_, rowButtons) ->
            val sorted = rowButtons.sortedBy { it.col }
            InlineKeyboardRow(
                sorted.map { button ->
                    InlineKeyboardButton(button.text).apply {
                        callbackData = objectMapper.writeValueAsString(button.payload)
                    }
                }
            )
        }
        return InlineKeyboardMarkup(rows)
    }
}
