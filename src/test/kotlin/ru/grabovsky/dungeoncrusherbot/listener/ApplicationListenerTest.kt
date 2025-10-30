package ru.grabovsky.dungeoncrusherbot.listener

import io.kotest.core.spec.style.ShouldSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.event.TelegramAdminMessageEvent
import ru.grabovsky.dungeoncrusherbot.event.TelegramReceiveCallbackEvent
import ru.grabovsky.dungeoncrusherbot.event.TelegramReceiveMessageEvent
import ru.grabovsky.dungeoncrusherbot.event.TelegramStateEvent
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.TelegramBotService
import ru.grabovsky.dungeoncrusherbot.strategy.context.LogicContext
import ru.grabovsky.dungeoncrusherbot.strategy.dto.AdminMessageDto
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.ExecuteStatus
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

class ApplicationListenerTest : ShouldSpec({

    val telegramBotService = mockk<TelegramBotService>()
    val stateService = mockk<StateService>()
    val logicContext = mockk<LogicContext>()

    val listener = ApplicationListener(telegramBotService, stateService, logicContext)

    val telegramUser = mockk<User>(relaxed = true) {
        every { id } returns 10L
        every { userName } returns "tester"
        every { firstName } returns "Tester"
    }

    val message = mockk<Message>(relaxed = true)

    beforeTest {
        clearMocks(telegramBotService, stateService, logicContext)
    }

    should("execute logic context for callback events") {
        val callbackEvent = TelegramReceiveCallbackEvent(telegramUser, StateCode.NOTIFY, "payload")
        every { logicContext.execute(telegramUser, callbackData = "payload", stateCode = StateCode.NOTIFY) } returns ExecuteStatus.FINAL

        listener.processCallbackEvent(callbackEvent)

        verify { logicContext.execute(telegramUser, callbackData = "payload", stateCode = StateCode.NOTIFY) }
    }

    should("process state event via telegram service") {
        val stateEvent = TelegramStateEvent(telegramUser, StateCode.WAITING)
        justRun { stateService.updateState(telegramUser, StateCode.WAITING) }
        justRun { telegramBotService.processState(telegramUser, StateCode.WAITING) }

        listener.processStateEvent(stateEvent)

        verify { stateService.updateState(telegramUser, StateCode.WAITING) }
        verify { telegramBotService.processState(telegramUser, StateCode.WAITING) }
    }

    should("forward admin messages without touching state") {
        val adminDto = AdminMessageDto("first", "tester", 10L, "body")
        val adminEvent = TelegramAdminMessageEvent(telegramUser, StateCode.WAITING, 777L, adminDto)
        justRun { telegramBotService.sendAdminMessage(777L, adminDto) }

        listener.processAdminMessageEvent(adminEvent)

        verify { telegramBotService.sendAdminMessage(777L, adminDto) }
        verify(exactly = 0) { telegramBotService.processState(any(), any()) }
    }

    should("execute logic context for message events") {
        val messageEvent = TelegramReceiveMessageEvent(telegramUser, StateCode.WAITING, message)
        justRun { logicContext.execute(telegramUser, message, StateCode.WAITING) }

        listener.processMessageEvent(messageEvent)

        verify { logicContext.execute(telegramUser, message, StateCode.WAITING) }
    }
})
