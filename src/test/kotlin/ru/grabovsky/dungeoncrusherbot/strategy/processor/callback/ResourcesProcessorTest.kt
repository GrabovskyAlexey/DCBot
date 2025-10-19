package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.resources.ResourcesProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class ResourcesProcessorTest : ShouldSpec({
    val stateService = mockk<StateService>()
    val processor = ResourcesProcessor(stateService)

    beforeTest {
        clearMocks(stateService)
    }

    should("store last selected server and return FINAL status") {
        val telegramUser = mockk<TgUser>(relaxed = true) {
            every { id } returns 101L
            every { firstName } returns "Tester"
        }
        val userState = UserState(userId = 101L, state = StateCode.RESOURCES)
        every { stateService.getState(telegramUser) } returns userState
        every { stateService.saveState(any()) } answers { firstArg() }

        val result = processor.execute(telegramUser, "RESOURCE 8")

        result shouldBe ExecuteStatus.FINAL
        userState.lastServerIdByState[StateCode.RESOURCES] shouldBe 8
        userState.callbackData shouldBe "RESOURCE 8"
        verify(atLeast = 1) { stateService.saveState(userState) }
    }
})
