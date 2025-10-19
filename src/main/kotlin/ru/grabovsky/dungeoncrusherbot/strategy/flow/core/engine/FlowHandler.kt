package ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine

import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import java.util.Locale

interface FlowHandler<TPayload : Any> {
    val key: FlowKey
    val payloadType: Class<TPayload>

    fun start(context: FlowStartContext): FlowResult<TPayload>

    fun onMessage(context: FlowMessageContext<TPayload>, message: Message): FlowResult<TPayload>?

    fun onCallback(context: FlowCallbackContext<TPayload>, callbackQuery: CallbackQuery, data: String): FlowResult<TPayload>?
}

data class FlowStartContext(
    val user: User,
    val locale: Locale,
)

data class FlowStateHolder<TPayload : Any>(
    val stepKey: String,
    val payload: TPayload,
    val messageBindings: Map<String, Int>,
)

data class FlowMessageContext<TPayload : Any>(
    val user: User,
    val locale: Locale,
    val state: FlowStateHolder<TPayload>,
)

data class FlowCallbackContext<TPayload : Any>(
    val user: User,
    val locale: Locale,
    val state: FlowStateHolder<TPayload>,
)

data class FlowResult<TPayload : Any>(
    val stepKey: String,
    val payload: TPayload,
    val actions: List<FlowAction> = emptyList(),
    val completed: Boolean = false,
)
