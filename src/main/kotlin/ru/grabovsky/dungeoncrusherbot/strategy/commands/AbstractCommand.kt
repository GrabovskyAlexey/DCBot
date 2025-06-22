package ru.grabovsky.dungeoncrusherbot.strategy.commands

import org.springframework.context.ApplicationEventPublisher
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.dungeoncrusherbot.event.TelegramStateEvent
import ru.grabovsky.dungeoncrusherbot.util.CommonUtils.currentStateCode

abstract class AbstractCommand(
    command: Command,
    private val eventPublisher: ApplicationEventPublisher
): BotCommand(command.command, command.text), BotCommands {
    fun classStateCode() = this.currentStateCode("Command")

    override fun execute(telegramClient: TelegramClient, user: User, chat: Chat, arguments: Array<out String>) {
        prepare(user, chat, arguments)

        eventPublisher.publishEvent(
            TelegramStateEvent(user, classStateCode())
        )
    }
}