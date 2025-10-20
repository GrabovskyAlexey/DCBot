package ru.grabovsky.dungeoncrusherbot.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.event.TelegramReceiveCallbackEvent
import ru.grabovsky.dungeoncrusherbot.event.TelegramReceiveMessageEvent
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowCallbackPayload
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKey
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowEngine
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ReceiverService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.state.MarkType
import ru.grabovsky.dungeoncrusherbot.util.LocaleUtils
import java.util.Locale

@Service
class ReceiverServiceImpl(
    private val stateService: StateService,
    private val userService: UserService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val objectMapper: ObjectMapper,
    private val flowEngine: FlowEngine,
) : ReceiverService {

    override fun execute(update: Update) {
        when {
            update.hasCallbackQuery() -> processCallback(update.callbackQuery)
            update.hasMessage() -> processMessage(update.message)
        }
    }

    private fun processMessage(message: Message) {
        val user = message.from
        userService.createOrUpdateUser(user)
        if (handleFlowMessage(user, message)) {
            return
        }
        val state = getState(user)
        if (state.state.markType == MarkType.DELETE) {
            state.deletedMessages.add(message.messageId)
            stateService.saveState(state)
        }
        applicationEventPublisher.publishEvent(
            TelegramReceiveMessageEvent(user, state.state, message)
        )
    }

    private fun processCallback(callbackQuery: CallbackQuery) {
        logger.debug {"Start process callback: $callbackQuery"}
        val user = callbackQuery.from
        userService.createOrUpdateUser(user)
        if (handleFlowCallback(user, callbackQuery)) {
            return
        }
        val state = getState(user)
        val event = runCatching {
            val data = objectMapper.readValue(callbackQuery.data, CallbackObject::class.java)
            return@runCatching TelegramReceiveCallbackEvent(user, data.state, data.data)
        }.onFailure {
            logger.error { "Error parsing CallbackObject from data: ${callbackQuery.data}" }
        }.getOrDefault(
            TelegramReceiveCallbackEvent(user, state.state, callbackQuery.data)
        )
        state.apply {
            this.state = event.stateCode
        }.also {
            stateService.saveState(it)
        }
        applicationEventPublisher.publishEvent(event)
    }

    private fun getState(user: User) =
        stateService.getState(user)

    private fun handleFlowMessage(user: User, message: Message): Boolean =
        flowEngine.onMessage(FlowKeys.RESOURCES, user, resolveLocale(user), message)

    private fun handleFlowCallback(user: User, callbackQuery: CallbackQuery): Boolean {
        val payload = parseFlowPayload(callbackQuery.data) ?: return false
        val flowKey = FlowKey(payload.flow)
        val locale = resolveLocale(user)
        if (flowEngine.onCallback(flowKey, user, locale, callbackQuery, payload.data)) {
            return true
        }
        return if (flowEngine.start(flowKey, user, locale)) {
            flowEngine.onCallback(flowKey, user, locale, callbackQuery, payload.data)
        } else {
            false
        }
    }

    private fun parseFlowPayload(data: String): FlowCallbackPayload? =
        runCatching {
            objectMapper.readValue(data, FlowCallbackPayload::class.java)
        }.getOrNull()

    private fun resolveLocale(user: User): Locale {
        val stored = userService.getUser(user.id)
        return LocaleUtils.resolve(stored?.language)
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}
