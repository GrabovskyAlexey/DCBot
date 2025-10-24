package ru.grabovsky.dungeoncrusherbot.strategy.flow.start

import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.*
import ru.grabovsky.dungeoncrusherbot.entity.User as BotUser

@Component
class StartFlow() : FlowHandler<Unit> {
    override val key: FlowKey = FlowKeys.START
    override val payloadType: Class<Unit> = Unit::class.java

    override fun start(context: FlowStartContext): FlowResult<Unit> {
        return FlowResult(
            stepKey = StartStep.MAIN.key,
            payload = Unit,
            actions = listOf(
                SendMessageAction(
                    bindingKey = MAIN_MESSAGE_BINDING,
                    message = FlowMessage(
                        flowKey = key,
                        stepKey = StartStep.MAIN.key,
                        model = StartViewModel(context.user.userName ?: context.user.firstName),
                    )
                )
            )
        )
    }

    override fun onMessage(context: FlowMessageContext<Unit>, message: Message): FlowResult<Unit>? = null

    override fun onCallback(context: FlowCallbackContext<Unit>, callbackQuery: CallbackQuery, data: String): FlowResult<Unit>? = null

    companion object {
        private const val MAIN_MESSAGE_BINDING = "start_main"
    }
}

data class StartViewModel(
    val username: String
)

enum class StartStep(override val key: String) : FlowStep {
    MAIN("main")
}
