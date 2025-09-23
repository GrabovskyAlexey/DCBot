package ru.grabovsky.dungeoncrusherbot

import org.springframework.context.MessageSource
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage

fun AbstractSendMessage<*>.setTestMessageSource(messageSource: MessageSource) {
    val field = AbstractSendMessage::class.java.getDeclaredField("messageSource")
    field.isAccessible = true
    field.set(this, messageSource)
}
