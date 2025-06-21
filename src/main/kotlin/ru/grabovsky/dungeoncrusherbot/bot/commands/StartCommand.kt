package ru.grabovsky.dungeoncrusherbot.bot.commands

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository
import ru.grabovsky.dungeoncrusherbot.service.MessageService
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Component
class StartCommand(
    private val userRepository: UserRepository,
    private val messageService: MessageService
) : AbstractCommand(Command.START) {
    override fun execute(
        telegramClient: TelegramClient, tgUser: TgUser, chat: Chat, arguments: Array<out String>
    ) {
        val user =
            userRepository.findUserByUserId(tgUser.id) ?:
            User(tgUser.id, tgUser.firstName, tgUser.lastName, tgUser.userName)
        userRepository.save(user)
        messageService.sendMessage(tgUser, chat, State.START)
    }
}