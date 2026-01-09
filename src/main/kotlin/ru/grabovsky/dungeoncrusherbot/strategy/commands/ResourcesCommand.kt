package ru.grabovsky.dungeoncrusherbot.strategy.commands

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowEngine
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Component
class ResourcesCommand(
    userService: UserService,
    private val flowEngine: FlowEngine,
) : AbstractCommand(Command.RESOURCES, FlowKeys.RESOURCES, userService, flowEngine)  {

    override fun prepare(user: TgUser, chat: Chat, arguments: Array<out String>) {
        super.prepare(user, chat, arguments)
        logger.debug { "Check resources for user: ${user.userName ?: user.firstName}" }
        val userFromDb = userService.getUser(user.id) ?: throw EntityNotFoundException("User with id: ${user.id} not found")
        logger.debug { "Resources flow is available for user: ${user.userName ?: user.firstName}" }
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}
