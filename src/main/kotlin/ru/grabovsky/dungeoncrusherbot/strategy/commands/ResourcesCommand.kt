package ru.grabovsky.dungeoncrusherbot.strategy.commands

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityNotFoundException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import ru.grabovsky.dungeoncrusherbot.entity.Resources
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Component
class ResourcesCommand(
    eventPublisher: ApplicationEventPublisher,
    userService: UserService
): AbstractCommand(Command.RESOURCES, eventPublisher, userService) {
    override fun prepare(user: TgUser, chat: Chat, arguments: Array<out String>) {
        super.prepare(user, chat, arguments)
        logger.info { "Check resources for user: ${user.userName ?: user.firstName}" }
        val userFromDb = userService.getUser(user.id) ?: throw EntityNotFoundException("User with id: ${user.id} not found")
        if (userFromDb.resources == null) {
            userFromDb.resources = Resources(user = userFromDb)
            userService.saveUser(userFromDb)
            logger.info { "Create resources for user: ${user.userName ?: user.firstName}" }
            return
        }
        logger.info { "Resources for user: ${user.userName ?: user.firstName} already exists" }
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}