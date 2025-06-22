package ru.grabovsky.dungeoncrusherbot.listener

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.event.TelegramEvent
import ru.grabovsky.dungeoncrusherbot.event.TelegramReceiveCallbackEvent
import ru.grabovsky.dungeoncrusherbot.event.TelegramReceiveMessageEvent
import ru.grabovsky.dungeoncrusherbot.event.TelegramStateEvent
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.TelegramBotService
import ru.grabovsky.dungeoncrusherbot.strategy.context.LogicContext
import ru.grabovsky.dungeoncrusherbot.strategy.context.StateContext
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.ExecuteStatus.FINAL
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.ExecuteStatus.NOTHING

@Component
class ApplicationListener(
    private val telegramBotService: TelegramBotService,
    private val stateService: StateService,
    private val stateContext: StateContext,
    private val logicContext: LogicContext,
) {
    @EventListener
    fun onTelegramEvent(event: TelegramEvent) {
        when (event) {
            is TelegramReceiveMessageEvent -> processMessageEvent(event)
            is TelegramStateEvent -> processStateEvent(event)
            is TelegramReceiveCallbackEvent -> processCallbackEvent(event)
        }
    }

    fun processMessageEvent(event: TelegramReceiveMessageEvent) {
        logicContext.execute(event.user, event.message, event.stateCode)

        stateContext.next(event.user, event.stateCode)?.let {
            processStateEvent(TelegramStateEvent(event.user, it))
        }
    }

    fun processStateEvent(event: TelegramStateEvent) {
        logger.debug { "Current state: ${event.stateCode.name}, pause: ${event.stateCode.pause}" }
        stateService.updateState(event.user, event.stateCode)
        telegramBotService.processState(event.user, event.stateCode)
    }

    fun processCallbackEvent(event: TelegramReceiveCallbackEvent) {
        when (logicContext.execute(event.user, event.callback, event.stateCode)) {
            FINAL -> { stateContext.next(event.user, event.stateCode) }
            NOTHING -> null
        }?.let {
            processStateEvent(TelegramStateEvent(event.user, it))
        }
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}
