package ru.grabovsky.dungeoncrusherbot.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.grabovsky.dungeoncrusherbot.strategy.dto.DataModel
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.CallbackProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.message.MessageProcessor

@Configuration
class TelegramConfig<T : DataModel>(
    private val sendMessages: List<AbstractSendMessage<T>>,
    private val messageProcessors: List<MessageProcessor>,
    private val callbackProcessors: List<CallbackProcessor>
) {
    @Bean
    fun sendMessages() = sendMessages.associateBy { it.classStateCode() }
    @Bean
    fun messageProcessors() = messageProcessors.associateBy { it.classStateCode() }
    @Bean
    fun callbackProcessors() = callbackProcessors.associateBy { it.classStateCode() }
}