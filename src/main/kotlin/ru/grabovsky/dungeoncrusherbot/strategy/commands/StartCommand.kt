package ru.grabovsky.dungeoncrusherbot.strategy.commands

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Component
class StartCommand(
    private val userService: UserService,
    eventPublisher: ApplicationEventPublisher,
) : AbstractCommand(Command.START, eventPublisher) {

    override fun prepare(user: TgUser, chat: Chat, arguments: Array<out String>) {
        userService.createOrUpdateUser(user)
    }
}