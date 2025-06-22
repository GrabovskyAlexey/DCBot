package ru.grabovsky.dungeoncrusherbot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.event.TelegramReceiveCallbackEvent
import ru.grabovsky.dungeoncrusherbot.event.TelegramReceiveMessageEvent
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ReceiverService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.state.MarkType

@Service
class ReceiverServiceImpl(
    private val stateService: StateService,
    private val applicationEventPublisher: ApplicationEventPublisher
): ReceiverService {

    override fun execute(update: Update) {
        when {
            update.hasCallbackQuery() -> processCallback(update.callbackQuery)
            update.hasMessage() -> processMessage(update.message)
        }
    }

    private fun processMessage(message: Message) {
        val user = message.from
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
        val user = callbackQuery.from
        val chat = callbackQuery.message.chat
        val state = getState(user)
        applicationEventPublisher.publishEvent(
            TelegramReceiveCallbackEvent(user, state.state, callbackQuery)
        )
    }

    private fun getState(user: User) =
        stateService.getState(user)

    companion object {
        val logger = KotlinLogging.logger {}
    }
}