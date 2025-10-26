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

    should("send admin message without changing state") {
        val adminDto = AdminMessageDto("first", "tester", 10L, "body")
        val adminEvent = TelegramAdminMessageEvent(telegramUser, StateCode.WAITING, 777L, adminDto)
        justRun { telegramBotService.sendAdminMessage(777L, adminDto) }

        listener.processAdminMessageEvent(adminEvent)

        verify { telegramBotService.sendAdminMessage(777L, adminDto) }
        verify(exactly = 0) { telegramBotService.processState(any(), any()) }
    }
})
