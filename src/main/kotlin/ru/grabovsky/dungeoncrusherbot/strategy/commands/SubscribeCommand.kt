package ru.grabovsky.dungeoncrusherbot.strategy.commands

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowEngine
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.util.LocaleUtils

@Component
class SubscribeCommand(
    userService: UserService,
    eventPublisher: ApplicationEventPublisher,
    private val flowEngine: FlowEngine,
) : AbstractCommand(Command.SUBSCRIBE, eventPublisher, userService) {

    override fun execute(
        telegramClient: TelegramClient,
        user: User,
        chat: Chat,
        arguments: Array<out String>
    ) {
        logger.info { "Process command ${classStateCode()} for user ${user.userName ?: user.firstName} with id ${user.id}" }
        runCatching {
            prepare(user, chat, arguments)
            val locale = LocaleUtils.resolve(userService.getUser(user.id)?.language ?: user.languageCode)
            if (!flowEngine.start(FlowKeys.SUBSCRIBE, user, locale)) {
                logger.error { "Flow ${FlowKeys.SUBSCRIBE} not found, command processing aborted" }
            }
        }.onFailure { error ->
            logger.info { "Error process command ${classStateCode()} for user ${user.userName ?: user.firstName} with id ${user.id} with error: $error, stacktrace: ${error.stackTrace}" }
        }
    }
}
