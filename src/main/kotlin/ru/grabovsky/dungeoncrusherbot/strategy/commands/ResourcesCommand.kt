package ru.grabovsky.dungeoncrusherbot.strategy.commands

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityNotFoundException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.dungeoncrusherbot.entity.Resources
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowEngine
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.util.LocaleUtils
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Component
class ResourcesCommand(
    userService: UserService,
    private val flowEngine: FlowEngine,
    eventPublisher: ApplicationEventPublisher,
) : AbstractCommand(Command.RESOURCES, eventPublisher, userService) {

    override fun execute(
        telegramClient: TelegramClient,
        user: User,
        chat: Chat,
        arguments: Array<out String>
    ) {
        val commandLabel = FlowKeys.RESOURCES.value
        logger.info { "Process flow $commandLabel for user ${user.userName ?: user.firstName} with id ${user.id}" }
        runCatching {
            prepare(user, chat, arguments)
            val locale = LocaleUtils.resolve(userService.getUser(user.id)?.language ?: user.languageCode)
            if (!flowEngine.start(FlowKeys.RESOURCES, user, locale)) {
                logger.error { "Flow ${FlowKeys.RESOURCES} not found, command processing aborted" }
            }
        }.onFailure { error ->
            logger.info { "Error process flow $commandLabel for user ${user.userName ?: user.firstName} with id ${user.id} with error: $error, stacktrace: ${error.stackTrace}" }
        }
    }

    override fun prepare(user: TgUser, chat: Chat, arguments: Array<out String>) {
        super.prepare(user, chat, arguments)
        logger.debug { "Check resources for user: ${user.userName ?: user.firstName}" }
        val userFromDb = userService.getUser(user.id) ?: throw EntityNotFoundException("User with id: ${user.id} not found")
        if (userFromDb.resources == null) {
            userFromDb.resources = Resources(user = userFromDb)
            userService.saveUser(userFromDb)
            logger.info { "Create resources for user: ${user.userName ?: user.firstName}" }
            return
        }
        logger.debug { "Resources for user: ${user.userName ?: user.firstName} already exists" }
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}
