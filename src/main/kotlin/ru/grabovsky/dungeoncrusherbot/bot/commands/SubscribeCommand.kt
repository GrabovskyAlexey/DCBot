package ru.grabovsky.dungeoncrusherbot.bot.commands

import org.springframework.stereotype.Component
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.dungeoncrusherbot.service.MessageService

@Component
class SubscribeCommand(
    private val messageService: MessageService
): AbstractCommand(Command.SUBSCRIBE)
{
    override fun execute(telegramClient: TelegramClient, user: User, chat: Chat, arguments: Array<out String>
    ) {
        messageService.sendMessage(user, chat, State.SUBSCRIBE)
    }
}