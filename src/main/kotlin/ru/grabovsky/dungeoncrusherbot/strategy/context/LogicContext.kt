package ru.grabovsky.dungeoncrusherbot.strategy.context

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.CallbackProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.ExecuteStatus
import ru.grabovsky.dungeoncrusherbot.strategy.processor.message.MessageProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

@Component
class LogicContext(
    private val messageProcessors: Map<StateCode, MessageProcessor>,
    private val callbackProcessors: Map<StateCode, CallbackProcessor>
) {

    fun execute(user: User, message: Message, stateCode: StateCode) {
        messageProcessors[stateCode]?.execute(user, message = message)
    }

    fun execute(user: User, callbackData: String, stateCode: StateCode): ExecuteStatus {
        return callbackProcessors[stateCode]
            ?.execute(user, callbackData = callbackData)
            ?: ExecuteStatus.NOTHING
                .also { logger.warn {"Callback not found with state: $stateCode" } }
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }

}