package ru.grabovsky.dungeoncrusherbot.strategy.commands

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowEngine
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKey
import ru.grabovsky.dungeoncrusherbot.util.LocaleUtils


abstract class AbstractFlowCommand(
    command: Command,
    protected val flowKey: FlowKey,
    userService: UserService,
    eventPublisher: ApplicationEventPublisher,
    private val flowEngine: FlowEngine,
) : AbstractCommand(command, eventPublisher, userService) {

    override fun execute(
        telegramClient: TelegramClient,
        user: User,
        chat: Chat,
        arguments: Array<out String>
    ) {
        logger.info { "Process flow ${flowKey.value} for user ${user.userName ?: user.firstName} with id ${user.id}" }
        runCatching {
            prepare(user, chat, arguments)
            val locale = LocaleUtils.resolve(userService.getUser(user.id)?.language ?: user.languageCode)
            if (!flowEngine.start(flowKey, user, locale)) {
                logger.error { "Flow $flowKey not found, command processing aborted" }
            }
        }.onFailure { error ->
            logger.info { "Error process flow ${flowKey.value} for user ${user.userName ?: user.firstName} with id ${user.id} with error: $error, stacktrace: ${error.stackTrace}" }
        }
    }
}
