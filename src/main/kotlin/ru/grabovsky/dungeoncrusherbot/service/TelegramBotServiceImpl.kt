package ru.grabovsky.dungeoncrusherbot.service

import com.fasterxml.jackson.databind.ObjectMapper
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
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.dto.ReplyMarkupDto
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.entity.NotifyHistory
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.entity.UpdateMessage
import ru.grabovsky.dungeoncrusherbot.entity.User as BotUser
import ru.grabovsky.dungeoncrusherbot.event.TelegramStateEvent
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.NotifyHistoryService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.TelegramBotService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.VerificationService
import ru.grabovsky.dungeoncrusherbot.strategy.context.MessageContext
import ru.grabovsky.dungeoncrusherbot.strategy.context.StateContext
import ru.grabovsky.dungeoncrusherbot.strategy.dto.AdminMessageDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.DataModel
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ReleaseNoteDto
import ru.grabovsky.dungeoncrusherbot.util.LocaleUtils
import ru.grabovsky.dungeoncrusherbot.strategy.state.MarkType
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateAction.*
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class TelegramBotServiceImpl(
    private val messageServiceGenerate: MessageGenerateService,
    private val telegramClient: TelegramClient,
    private val stateContext: StateContext,
    private val messageContext: MessageContext<DataModel>,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val stateService: StateService,
    private val userService: UserService,
    private val notifyHistoryService: NotifyHistoryService,
    private val verificationService: VerificationService,
    private val objectMapper: ObjectMapper
) : TelegramBotService {

    override fun processState(user: User, stateCode: StateCode) {
        when (stateCode.action) {
            SEND_MESSAGE -> sendMessage(user, stateCode)
            UPDATE_MESSAGE -> editMessage(user, stateCode)
            DELETE_MESSAGES -> deleteMessage(user)
            VERIFICATION -> verify(user, stateCode)
            NOTHING -> {}
        }

        if (stateCode.pause) {
            return
        }
        stateContext.next(user, stateCode)?.let { state ->
            applicationEventPublisher.publishEvent(TelegramStateEvent(user, state))
        }
    }

    private fun sendMessage(user: User, stateCode: StateCode) {
        val result = telegramClient.execute(getSendMessage(user, stateCode))
        val state = stateService.getState(user)
        when (stateCode.markType) {
            MarkType.DELETE -> state.deletedMessages.add(result.messageId)
            MarkType.UPDATE -> state.updateMessageByState[stateCode] = result.messageId
            else -> {}
        }
        stateService.saveState(state)
    }

    private fun editMessage(user: User, stateCode: StateCode) {
        val state = stateService.getState(user)

        val messageId = state.updateMessageByState[stateCode.linkedStateCode]
            ?: state.updateMessageId
        requireNotNull(messageId) {"Not found update message id for user: ${user.userName ?: user.firstName} and state: $stateCode"}
        telegramClient.execute(getEditMessage(user, stateCode, messageId))
        stateService.saveState(state)
    }

    private fun deleteMessage(user: User) {
        val state = stateService.getState(user)
        if (state.deletedMessages.isNotEmpty()) {
            telegramClient.execute(getDeleteMessages(user.id, state.deletedMessages))
        }
        state.deletedMessages.clear()
        stateService.saveState(state)
    }


    private fun getEditMessage(user: User, stateCode: StateCode, messageId: Int): EditMessageText {
        logger.info { "Get update message for state: $stateCode, user: ${user.userName ?: user.firstName}" }
        val locale = resolveLocale(user)
        val message = messageContext.getMessage(user, stateCode, locale)
            ?: throw IllegalStateException("message is null")

        val markup = message.inlineButtons.getInlineKeyboardMarkup()

        val editMessage = EditMessageText.builder()
            .chatId(user.id)
            .text(message.message)
            .replyMarkup(markup)
            .messageId(messageId)
            .build()
        editMessage.enableMarkdown(true)
        logger.debug { "Edit message: $editMessage" }

        return editMessage
    }

    private fun getDeleteMessages(chatId: Long, messageIds: List<Int>): DeleteMessages {
        return DeleteMessages.builder()
            .chatId(chatId)
            .messageIds(messageIds)
            .build()
    }

    private fun verify(user: User, stateCode: StateCode) {
        verificationService.verify(user, stateCode)
    }

    private fun resolveLocale(user: User) = LocaleUtils.resolve(userService.getUser(user.id)?.language)

    private fun getSendMessage(user: User, stateCode: StateCode): SendMessage {
        logger.info { "Get send message for state: $stateCode, user: ${user.userName ?: user.firstName}" }
        val locale = resolveLocale(user)
        val message = messageContext.getMessage(user, stateCode, locale)
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
                button.callbackData = objectMapper.writeValueAsString(markup.data)
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

    override fun sendNotification(chatId: Long, type: NotificationType, servers: List<Server>, isBefore: Boolean?): Boolean {
        val message = when (type) {
            NotificationType.SIEGE -> {
                if (servers.isEmpty()) {
                    return false
                }
                messageServiceGenerate.process(
                    StateCode.NOTIFICATION_SIEGE,
                    ServerDto(servers.sortedBy { it.id }.map { it.id }, isBefore)
                )
            }

            NotificationType.MINE -> messageServiceGenerate.process(StateCode.NOTIFICATION_MINE)
        }

        val sendMessage = SendMessage.builder()
            .chatId(chatId)
            .text(message)
            .build()

        sendMessage.enableMarkdown(true)

        return runCatching {
            telegramClient.execute(sendMessage)
                .also { result ->
                    NotifyHistory(
                        userId = chatId,
                        messageId = result.messageId,
                        text = result.text,
                        sendTime = Instant.now()
                    ).also {
                        notifyHistoryService.saveHistory(it)
                    }
                }
        }.onFailure {
            error ->
            logger.warn { "Notification send error: $error" }
            return !(error is TelegramApiRequestException && error.errorCode == 403)
        }.isSuccess
    }

    override fun deleteOldNotify() {
        logger.info { "Start deleting old messages" }
        val deleteTimestamp = Instant.now().minus(2, ChronoUnit.HOURS)
        val messageForDelete = notifyHistoryService.getNotDeletedHistoryEvent()
            .filter { it.userId != null }
            .filter { it.sendTime.isBefore(deleteTimestamp) }
            .groupBy { it.userId!! }
        for (entry in messageForDelete) {
            if(entry.value.isEmpty()) continue
            val deleteMessage = getDeleteMessages(entry.key, entry.value.map { it.messageId })
            runCatching {
                logger.debug { "Start delete message for userId: ${deleteMessage.chatId} messageIds: ${deleteMessage.messageIds}" }
                telegramClient.execute(deleteMessage)
                notifyHistoryService.markAsDeleted(entry.value)
            }.onSuccess {
                logger.debug { "Success delete message for userId: ${deleteMessage.chatId} messageIds: ${deleteMessage.messageIds}" }
            }.onFailure { error ->
                logger.info { "Error delete message for userId: ${deleteMessage.chatId} messageIds: ${deleteMessage.messageIds} with message ${error.message}" }
            }
        }
        logger.info { "Finish deleting old messages" }
    }

    override fun sendReleaseNotes(
        user: BotUser,
        updateMessage: UpdateMessage
    ) {
        val locale = LocaleUtils.resolve(user.language)
        val releaseNoteDto = ReleaseNoteDto(
            version = updateMessage.version,
            text = updateMessage.text,
            textEn = updateMessage.textEn
        )
        val message = messageServiceGenerate.process(StateCode.RELEASE_NOTES, releaseNoteDto, locale)
        val sendMessage = SendMessage.builder()
            .chatId(user.userId)
            .text(message)
            .build()
        sendMessage.enableMarkdown(true)
        telegramClient.execute(sendMessage)
    }

    override fun sendAdminMessage(
        chatId: Long,
        dto: AdminMessageDto
    ) {
        val message = messageServiceGenerate.process(StateCode.ADMIN_MESSAGE, dto)
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