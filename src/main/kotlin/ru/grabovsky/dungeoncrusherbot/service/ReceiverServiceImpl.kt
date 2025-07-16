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
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ReceiverService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.state.MarkType

@Service
class ReceiverServiceImpl(
    private val stateService: StateService,
    private val userService: UserService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val objectMapper: ObjectMapper
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

    companion object {
        val logger = KotlinLogging.logger {}
    }
}