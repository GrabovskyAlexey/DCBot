package ru.grabovsky.dungeoncrusherbot.listener

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.event.TelegramAdminMessageEvent
import ru.grabovsky.dungeoncrusherbot.event.TelegramReceiveCallbackEvent
import ru.grabovsky.dungeoncrusherbot.event.TelegramReceiveMessageEvent
import ru.grabovsky.dungeoncrusherbot.event.TelegramStateEvent
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.TelegramBotService
import ru.grabovsky.dungeoncrusherbot.strategy.context.LogicContext
import ru.grabovsky.dungeoncrusherbot.strategy.context.StateContext
import ru.grabovsky.dungeoncrusherbot.strategy.dto.AdminMessageDto
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.ExecuteStatus
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

class ApplicationListenerTest : ShouldSpec({

    val telegramBotService = mockk<TelegramBotService>()
    val stateService = mockk<StateService>()
    val stateContext = mockk<StateContext>()
    val logicContext = mockk<LogicContext>()

    val listener = ApplicationListener(telegramBotService, stateService, stateContext, logicContext)

    val telegramUser = mockk<User>(relaxed = true) {
        every { id } returns 10L
        every { userName } returns "tester"
        every { firstName } returns "Tester"
    }

    val message = mockk<Message>(relaxed = true)

    beforeTest {
        clearMocks(telegramBotService, stateService, stateContext, logicContext)
    }

    should("update state and delegate to telegram service on processStateEvent") {
        val event = TelegramStateEvent(telegramUser, StateCode.START)
        justRun { stateService.updateState(telegramUser, StateCode.START) }
        justRun { telegramBotService.processState(telegramUser, StateCode.START) }

        listener.processStateEvent(event)

        verify { stateService.updateState(telegramUser, StateCode.START) }
        verify { telegramBotService.processState(telegramUser, StateCode.START) }
    }

    should("execute logic and chain next state for message events") {
        val messageEvent = TelegramReceiveMessageEvent(telegramUser, StateCode.START, message)
        justRun { logicContext.execute(telegramUser, message = message, stateCode = StateCode.START) }
        every { stateContext.next(telegramUser, StateCode.START) } returns StateCode.WAITING
        justRun { stateService.updateState(telegramUser, StateCode.WAITING) }
        justRun { telegramBotService.processState(telegramUser, StateCode.WAITING) }

        listener.processMessageEvent(messageEvent)

        verify { logicContext.execute(telegramUser, message = message, stateCode = StateCode.START) }
        verify { stateContext.next(telegramUser, StateCode.START) }
        verify { telegramBotService.processState(telegramUser, StateCode.WAITING) }
    }

    should("dispatch callback final result to next state") {
        val callbackEvent = TelegramReceiveCallbackEvent(telegramUser, StateCode.MAZE, "payload")
        every { logicContext.execute(telegramUser, callbackData = "payload", stateCode = StateCode.MAZE) } returns ExecuteStatus.FINAL
        every { stateContext.next(telegramUser, StateCode.MAZE) } returns StateCode.UPDATE_MAZE
        justRun { stateService.updateState(telegramUser, StateCode.UPDATE_MAZE) }
        justRun { telegramBotService.processState(telegramUser, StateCode.UPDATE_MAZE) }

        listener.processCallbackEvent(callbackEvent)

        verify { logicContext.execute(telegramUser, callbackData = "payload", stateCode = StateCode.MAZE) }
        verify { stateContext.next(telegramUser, StateCode.MAZE) }
        verify { telegramBotService.processState(telegramUser, StateCode.UPDATE_MAZE) }
    }

    should("skip state change when callback processor returns nothing") {
        val callbackEvent = TelegramReceiveCallbackEvent(telegramUser, StateCode.SETTINGS, "noop")
        every { logicContext.execute(telegramUser, callbackData = "noop", stateCode = StateCode.SETTINGS) } returns ExecuteStatus.NOTHING

        listener.processCallbackEvent(callbackEvent)

        verify(exactly = 0) { telegramBotService.processState(any(), any()) }
    }

    should("send admin message and switch to report complete state") {
        val adminDto = AdminMessageDto("first", "tester", 10L, "body")
        val adminEvent = TelegramAdminMessageEvent(telegramUser, StateCode.SEND_REPORT, 777L, adminDto)
        justRun { telegramBotService.sendAdminMessage(777L, adminDto) }
        justRun { stateService.updateState(telegramUser, StateCode.SEND_REPORT_COMPLETE) }
        justRun { telegramBotService.processState(telegramUser, StateCode.SEND_REPORT_COMPLETE) }

        listener.processAdminMessageEvent(adminEvent)

        verify { telegramBotService.sendAdminMessage(777L, adminDto) }
        verify { telegramBotService.processState(telegramUser, StateCode.SEND_REPORT_COMPLETE) }
    }

    should("route events via onTelegramEvent dispatcher") {
        val event = TelegramStateEvent(telegramUser, StateCode.START)
        justRun { stateService.updateState(telegramUser, StateCode.START) }
        justRun { telegramBotService.processState(telegramUser, StateCode.START) }

        listener.onTelegramEvent(event)

        verify { telegramBotService.processState(telegramUser, StateCode.START) }
    }
})
