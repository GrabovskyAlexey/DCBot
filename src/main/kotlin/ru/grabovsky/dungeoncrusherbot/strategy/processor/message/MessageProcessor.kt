package ru.grabovsky.dungeoncrusherbot.strategy.processor.message

import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.strategy.processor.Processor

interface MessageProcessor: Processor {
    fun execute(user: User, message: Message)
}