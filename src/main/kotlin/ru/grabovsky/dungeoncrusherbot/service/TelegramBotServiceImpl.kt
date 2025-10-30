package ru.grabovsky.dungeoncrusherbot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.entity.NotifyHistory
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.entity.UpdateMessage
import ru.grabovsky.dungeoncrusherbot.entity.User as BotUser
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.NotifyHistoryService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.TelegramBotService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.AdminMessageDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ReleaseNoteDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.util.LocaleUtils
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class TelegramBotServiceImpl(
    private val messageServiceGenerate: MessageGenerateService,
    private val telegramClient: TelegramClient,
    private val stateService: StateService,
    private val userService: UserService,
    private val notifyHistoryService: NotifyHistoryService,
) : TelegramBotService {

    override fun processState(user: User, stateCode: StateCode) {
        logger.debug { "Persist state $stateCode for user ${user.id}" }
        stateService.updateState(user, stateCode)
    }

    override fun sendNotification(
        chatId: Long,
        type: NotificationType,
        servers: List<Server>,
        isBefore: Boolean?
    ): Boolean {
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
            .also { it.enableMarkdown(true) }

        return runCatching {
            telegramClient.execute(sendMessage).also { result ->
                NotifyHistory(
                    userId = chatId,
                    messageId = result.messageId,
                    text = result.text,
                    sendTime = Instant.now()
                ).also(notifyHistoryService::saveHistory)
            }
        }.onFailure { error ->
            logger.warn { "Notification to chatId=$chatId failed: ${error.message}" }
            if (error is TelegramApiRequestException && error.errorCode == 403) {
                return false
            }
        }.isSuccess
    }

    override fun deleteOldNotify() {
        logger.debug { "Start deleting old notifications" }
        val deleteBefore = Instant.now().minus(2, ChronoUnit.HOURS)
        val grouped = notifyHistoryService.getNotDeletedHistoryEvent()
            .filter { it.userId != null && it.sendTime.isBefore(deleteBefore) }
            .groupBy { it.userId!! }

        grouped.forEach { (userId, history) ->
            if (history.isEmpty()) return@forEach
            val deleteCommand = DeleteMessages.builder()
                .chatId(userId)
                .messageIds(history.map { it.messageId })
                .build()

            runCatching { telegramClient.execute(deleteCommand) }
                .onSuccess {
                    notifyHistoryService.markAsDeleted(history)
                    logger.debug { "Deleted old notifications for userId=$userId" }
                }
                .onFailure { error ->
                    logger.info { "Failed to delete notifications for userId=$userId: ${error.message}" }
                }
        }
        logger.debug { "Finish deleting old notifications" }
    }

    override fun sendReleaseNotes(user: BotUser, updateMessage: UpdateMessage) {
        val locale = LocaleUtils.resolve(user.language)
        val dto = ReleaseNoteDto(
            version = updateMessage.version,
            text = updateMessage.text,
            textEn = updateMessage.textEn
        )
        val message = messageServiceGenerate.process(StateCode.RELEASE_NOTES, dto, locale)
        SendMessage.builder()
            .chatId(user.userId)
            .text(message)
            .build()
            .also { it.enableMarkdown(true) }
            .let(telegramClient::execute)
    }

    override fun sendAdminMessage(chatId: Long, dto: AdminMessageDto) {
        val message = messageServiceGenerate.process(StateCode.ADMIN_MESSAGE, dto)
        SendMessage.builder()
            .chatId(chatId)
            .text(message)
            .build()
            .also { it.enableMarkdown(true) }
            .let(telegramClient::execute)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
