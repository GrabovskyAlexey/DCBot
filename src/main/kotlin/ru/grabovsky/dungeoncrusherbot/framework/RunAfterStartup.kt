package ru.grabovsky.dungeoncrusherbot.framework

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
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
            users.forEach { user ->
                runCatching {
                    telegramBotService.sendReleaseNotes(user, message)
                }.onFailure { error ->
                    if (error is TelegramApiRequestException && error.errorCode == 403) {
                        user.profile?.let {
                            it.isBlocked = true
                            userRepository.save(user)
                        }
                    }
                    logger.warn { "Couldn't send update message version: ${message.version} for user: $user with error: ${error.message}" }
                }
            }
            message.sent = true
        }
        updateMessageRepository.saveAll(updateMessages)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
