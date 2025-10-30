package ru.grabovsky.dungeoncrusherbot.listener

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.event.TelegramAdminMessageEvent
import ru.grabovsky.dungeoncrusherbot.event.TelegramEvent
import ru.grabovsky.dungeoncrusherbot.event.TelegramReceiveCallbackEvent
import ru.grabovsky.dungeoncrusherbot.event.TelegramReceiveMessageEvent
import ru.grabovsky.dungeoncrusherbot.event.TelegramStateEvent
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.TelegramBotService
import ru.grabovsky.dungeoncrusherbot.strategy.context.LogicContext

@Component
class ApplicationListener(
    private val telegramBotService: TelegramBotService,
    private val stateService: StateService,
    private val logicContext: LogicContext,
) {
    @EventListener
    fun onTelegramEvent(event: TelegramEvent) {
        when (event) {
            is TelegramReceiveMessageEvent -> processMessageEvent(event)
            is TelegramStateEvent -> processStateEvent(event)
            is TelegramReceiveCallbackEvent -> processCallbackEvent(event)
            is TelegramAdminMessageEvent -> processAdminMessageEvent(event)
        }
    }

    fun processAdminMessageEvent(event: TelegramAdminMessageEvent) {
        logger.info { "Process admin message with chatId:${event.adminChatId}, message: ${event.dto}" }
        telegramBotService.sendAdminMessage(event.adminChatId, event.dto)
    }

    fun processMessageEvent(event: TelegramReceiveMessageEvent) {
        logicContext.execute(event.user, event.message, event.stateCode)
    }

    fun processStateEvent(event: TelegramStateEvent) {
        logger.debug { "Current state: ${event.stateCode.name}, pause: ${event.stateCode.pause}" }
        stateService.updateState(event.user, event.stateCode)
        telegramBotService.processState(event.user, event.stateCode)
    }

    fun processCallbackEvent(event: TelegramReceiveCallbackEvent) {
        logicContext.execute(event.user, event.callbackData, event.stateCode)
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}
