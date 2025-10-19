package ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.service.interfaces.FlowStateService
import java.util.Locale

@Component
class FlowEngine(
    handlers: List<FlowHandler<out Any>>,
    private val stateService: FlowStateService,
    private val serializer: FlowPayloadSerializer,
    private val actionExecutor: FlowActionExecutor,
) {
    private val handlerByKey: Map<FlowKey, FlowHandler<out Any>> = handlers.associateBy { it.key }

    fun start(flowKey: FlowKey, user: User, locale: Locale): Boolean {
        val handler = handler<Any>(flowKey) ?: return false
        stateService.clear(user.id, flowKey)
        val result = handler.start(FlowStartContext(user, locale))
        applyResult(user, flowKey, null, handler, result, locale)
        return true
    }

    fun onMessage(flowKey: FlowKey, user: User, locale: Locale, message: Message): Boolean {
        val handler = handler<Any>(flowKey) ?: return false
        val snapshot = stateService.load(user.id, flowKey) ?: return false
        val context = FlowMessageContext(
            user = user,
            locale = locale,
            state = snapshot.toStateHolder(handler),
        )
        val result = handler.onMessage(context, message) ?: return false
        applyResult(user, flowKey, snapshot, handler, result, locale)
        return true
    }

    fun onCallback(flowKey: FlowKey, user: User, locale: Locale, callbackQuery: CallbackQuery, data: String): Boolean {
        val handler = handler<Any>(flowKey) ?: return false
        val snapshot = stateService.load(user.id, flowKey) ?: return false
        val context = FlowCallbackContext(
            user = user,
            locale = locale,
            state = snapshot.toStateHolder(handler),
        )
        val result = handler.onCallback(context, callbackQuery, data) ?: return false
        applyResult(user, flowKey, snapshot, handler, result, locale)
        return true
    }

    private fun FlowStateSnapshot.toMutableBindings(): MutableMap<String, Int> =
        messageBindings.toMutableMap()

    private fun <TPayload : Any> FlowStateSnapshot.toStateHolder(handler: FlowHandler<TPayload>): FlowStateHolder<TPayload> {
        val payload = serializer.deserialize(payload, handler.payloadType)
        return FlowStateHolder(
            stepKey = stepKey,
            payload = payload,
            messageBindings = messageBindings,
        )
    }

    private fun <TPayload : Any> applyResult(
        user: User,
        flowKey: FlowKey,
        previousSnapshot: FlowStateSnapshot?,
        handler: FlowHandler<TPayload>,
        result: FlowResult<TPayload>,
        locale: Locale,
    ) {
        val currentBindings = previousSnapshot?.messageBindings ?: emptyMap()
        val mutation = actionExecutor.execute(user, locale, currentBindings, result.actions)
        val updatedBindings = currentBindings
            .toMutableMap()
            .apply {
                putAll(mutation.replacements)
                mutation.removed.forEach { remove(it) }
            }

        if (result.completed) {
            stateService.clear(user.id, flowKey)
            return
        }

        val payloadJson = serializer.serialize(result.payload)
        val snapshot = FlowStateSnapshot(
            userId = user.id,
            flowKey = flowKey,
            stepKey = result.stepKey,
            payload = payloadJson,
            messageBindings = updatedBindings,
        )
        stateService.save(snapshot)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <TPayload : Any> handler(flowKey: FlowKey): FlowHandler<TPayload>? =
        handlerByKey[flowKey] as? FlowHandler<TPayload>
}