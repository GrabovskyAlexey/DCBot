package ru.grabovsky.dungeoncrusherbot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.entity.NotifyHistory
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.entity.UpdateMessage
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.NotifyHistoryService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.TelegramBotService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ReleaseNoteDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerDto
import ru.grabovsky.dungeoncrusherbot.util.LocaleUtils
import java.time.Instant
import java.time.temporal.ChronoUnit
import ru.grabovsky.dungeoncrusherbot.entity.User as BotUser

@Service
class TelegramBotServiceImpl(
    private val messageServiceGenerate: MessageGenerateService,
    private val telegramClient: TelegramClient,
    private val notifyHistoryService: NotifyHistoryService,
) : TelegramBotService {

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
                messageServiceGenerate.processTemplate(
                    SIEGE_NOTIFICATION_TEMPLATE,
                    ServerDto(servers.sortedBy { it.id }.map { it.id }, isBefore)
                )
            }

            NotificationType.MINE -> messageServiceGenerate.processTemplate(MINE_NOTIFICATION_TEMPLATE)
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
        val message = messageServiceGenerate.processTemplate(RELEASE_NOTES_TEMPLATE, dto, locale)
        SendMessage.builder()
            .chatId(user.userId)
            .text(message)
            .build()
            .also { it.enableMarkdown(true) }
            .let(telegramClient::execute)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
        private const val SIEGE_NOTIFICATION_TEMPLATE = "notification/siege"
        private const val MINE_NOTIFICATION_TEMPLATE = "notification/mine"
        private const val RELEASE_NOTES_TEMPLATE = "release_notes/main"
    }
}
