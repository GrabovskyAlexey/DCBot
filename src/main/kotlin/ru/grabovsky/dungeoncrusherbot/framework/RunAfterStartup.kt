package ru.grabovsky.dungeoncrusherbot.framework

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.entity.NotificationSubscribe
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.repository.UpdateMessageRepository
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.TelegramBotService

@Component
class RunAfterStartup(
    private val telegramBotService: TelegramBotService,
    private val userRepository: UserRepository,
    private val updateMessageRepository: UpdateMessageRepository
) {
    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun runAfterStartup() {
        logger.info { "Start process update messages" }
        val updateMessages = updateMessageRepository.findUpdateMessagesBySentNot()
        if (updateMessages.isEmpty()) {
            logger.info { "Update messages is empty" }
            return
        }
        val users = userRepository.findAllNotBlockedUser()
        for (message in updateMessages) {
            users.forEach {
                runCatching {
                    telegramBotService.sendReleaseNotes(it.userId, message)
                }.onFailure { error ->
                    logger.warn { "Couldn't send update message version: ${message.version} for user: $it with error: ${error.message}" }
                    it.isBlocked = true
                    userRepository.save(it)
                }
            }
            message.sent = true
        }
        updateMessageRepository.saveAll(updateMessages)
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}