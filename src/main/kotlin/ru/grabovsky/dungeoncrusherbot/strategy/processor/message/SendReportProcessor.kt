package ru.grabovsky.dungeoncrusherbot.strategy.processor.message

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Component
class SendReportProcessor(private val userService: UserService) : MessageProcessor {
    override fun execute(user: User, message: Message) {
        userService.sendAdminMessage(user, message.text)
    }
}