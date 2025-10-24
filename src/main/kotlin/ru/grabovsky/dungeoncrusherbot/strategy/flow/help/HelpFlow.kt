package ru.grabovsky.dungeoncrusherbot.strategy.flow.help

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.*

@Component
class HelpFlow() : FlowHandler<Unit> {
    override val key: FlowKey = FlowKeys.HELP
    override val payloadType: Class<Unit> = Unit::class.java

    override fun start(context: FlowStartContext): FlowResult<Unit> {
        return FlowResult(
            stepKey = StepKey.MAIN.key,
            payload = Unit,
            actions = listOf(
                SendMessageAction(
                    bindingKey = MAIN_MESSAGE_BINDING,
                    message = FlowMessage(
                        flowKey = key,
                        stepKey = StepKey.MAIN.key,
                    )
                )
            )
        )
    }

    override fun onMessage(context: FlowMessageContext<Unit>, message: Message): FlowResult<Unit>? = null

    override fun onCallback(context: FlowCallbackContext<Unit>, callbackQuery: CallbackQuery, data: String): FlowResult<Unit>? = null

    companion object {
        private const val MAIN_MESSAGE_BINDING = "help_main"
    }
}
