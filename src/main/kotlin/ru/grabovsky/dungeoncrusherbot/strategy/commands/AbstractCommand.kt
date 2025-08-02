package ru.grabovsky.dungeoncrusherbot.strategy.commands

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.dungeoncrusherbot.event.TelegramStateEvent
import ru.grabovsky.dungeoncrusherbot.util.CommonUtils.currentStateCode

abstract class AbstractCommand(
    command: Command,
    private val eventPublisher: ApplicationEventPublisher,
    val sortOrder: Int = command.order
): BotCommand(command.command, command.text), BotCommands {
    fun classStateCode() = this.currentStateCode("Command")

    override fun execute(telegramClient: TelegramClient, user: User, chat: Chat, arguments: Array<out String>) {
        logger.info { "Process command ${classStateCode()} for user ${user.userName?:user.firstName} with id ${user.id}" }
        runCatching {
            prepare(user, chat, arguments)

            eventPublisher.publishEvent(
                TelegramStateEvent(user, classStateCode())
            )
        }.onFailure { error ->
            logger.info { "Error process command ${classStateCode()} for user ${user.userName?:user.firstName} with id ${user.id} with error: $error, stacktrace: ${error.stackTrace}" }
        }
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}