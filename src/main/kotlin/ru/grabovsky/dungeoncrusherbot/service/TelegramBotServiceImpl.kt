package ru.grabovsky.dungeoncrusherbot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.event.TelegramStateEvent
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.TelegramBotService
import ru.grabovsky.dungeoncrusherbot.strategy.context.MessageContext
import ru.grabovsky.dungeoncrusherbot.strategy.context.StateContext
import ru.grabovsky.dungeoncrusherbot.strategy.dto.DataModel
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.MarkType
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateAction.*
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.poibot.dto.InlineMarkupDataDto
import ru.grabovsky.poibot.dto.ReplyMarkupDto

@Service
class TelegramBotServiceImpl(
    private val messageServiceGenerate: MessageGenerateServiceImpl,
    private val telegramClient: TelegramClient,
    private val stateContext: StateContext,
    private val messageContext: MessageContext<DataModel>,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val stateService: StateService
): TelegramBotService {

    override fun processState(user: User, stateCode: StateCode) {
        when (stateCode.action) {
            SEND_MESSAGE -> sendMessage(user, stateCode)
            UPDATE_MESSAGE -> editMessage(user, stateCode)
            DELETE_MESSAGES -> deleteMessage(user)
            NOTHING -> {}
        }

        if (stateCode.pause) {
            return
        }
        stateContext.next(user, stateCode)?.let { stateCode ->
            applicationEventPublisher.publishEvent(TelegramStateEvent(user, stateCode))
        }
    }

    private fun sendMessage(user: User, stateCode: StateCode) {
        val result = telegramClient.execute(getSendMessage(user, stateCode))
        val state = stateService.getState(user)
        when(stateCode.markType) {
            MarkType.DELETE -> state.deletedMessages.add(result.messageId)
            MarkType.UPDATE -> state.updateMessageId = result.messageId
            else -> {}
        }
        stateService.saveState(state)
    }

    private fun editMessage(user: User, stateCode: StateCode) {
        val state = stateService.getState(user)
        state.updateMessageId?.let {
            telegramClient.execute(getEditMessage(user, stateCode, it))
        }
        stateService.saveState(state)
    }

    private fun deleteMessage(user: User) {
        val state = stateService.getState(user)
        if(state.deletedMessages.isNotEmpty()) {
            telegramClient.execute(getDeleteMessages(user.id, state.deletedMessages))
        }
        state.deletedMessages.clear()
        stateService.saveState(state)
    }


    private fun getEditMessage(user: User, stateCode: StateCode, messageId: Int): EditMessageText {
        logger.info { "Get update message for state: $stateCode" }
        val message = messageContext.getMessage(user, stateCode)
            ?: throw IllegalStateException("message is null")

        val markup = message.inlineButtons.getInlineKeyboardMarkup()

        val editMessage = EditMessageText.builder()
            .chatId(user.id)
            .text(message.message)
            .replyMarkup(markup)
            .messageId(messageId)
            .build()
        editMessage.enableMarkdown(true)
        logger.info { "Edit message: $editMessage" }

        return editMessage
    }

    private fun getDeleteMessages(chatId: Long, messageIds: List<Int>): DeleteMessages {
        return DeleteMessages.builder()
            .chatId(chatId)
            .messageIds(messageIds)
            .build()
    }

    private fun getSendMessage(user: User, stateCode: StateCode): SendMessage {
        logger.info { "Get send message for state: $stateCode" }
        val message = messageContext.getMessage(user, stateCode)
            ?: throw IllegalStateException("message is null")

        val markup = message.inlineButtons.getInlineKeyboardMarkup()
            .takeIf { it.keyboard.isNotEmpty() }
            ?: message.replyButtons.takeIf { it.isNotEmpty() }?.getReplyMarkup()
            ?: ReplyKeyboardRemove(true)

        val sendMessage = SendMessage.builder()
            .chatId(user.id)
            .text(message.message)
            .replyMarkup(markup)
            .build()

        sendMessage.enableMarkdown(true)
        return sendMessage
    }

    private fun List<InlineMarkupDataDto>.getInlineKeyboardMarkup(): InlineKeyboardMarkup {

        var inlineKeyboardButtonsInner: MutableList<InlineKeyboardButton>
        val inlineKeyboardButtons: MutableList<MutableList<InlineKeyboardButton>> = mutableListOf()

        this.groupBy { it.rowPos }.toSortedMap().forEach { entry: Map.Entry<Int, List<InlineMarkupDataDto>> ->
            inlineKeyboardButtonsInner = mutableListOf()
            entry.value.forEach { markup: InlineMarkupDataDto ->
                val button = InlineKeyboardButton(markup.text)
                button.callbackData = markup.data
                inlineKeyboardButtonsInner.add(button)
            }
            inlineKeyboardButtons.add(inlineKeyboardButtonsInner.toMutableList())
        }
        val rows = inlineKeyboardButtons.map { InlineKeyboardRow(it) }
        return InlineKeyboardMarkup(rows)
    }

    private fun List<ReplyMarkupDto>.getReplyMarkup(): ReplyKeyboard {
        val keyboardRows = this.map { rmd ->
            KeyboardRow(
                KeyboardButton.builder()
                    .text(rmd.text)
                    .requestLocation(rmd.requestLocation)
                    .build()
            )
        }
        return ReplyKeyboardMarkup.builder().keyboard(keyboardRows).build()
    }

    override fun sendNotification(chatId: Long, servers: List<Server>){
        if(servers.isEmpty()) {
            return
        }
        val message = messageServiceGenerate.process(StateCode.NOTIFICATION,
            ServerDto(servers.sortedBy { it.id }.map { it.id })
        )
        val sendMessage = SendMessage.builder()
            .chatId(chatId)
            .text(message)
            .build()

        sendMessage.enableMarkdown(true)

        telegramClient.execute(sendMessage)
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}