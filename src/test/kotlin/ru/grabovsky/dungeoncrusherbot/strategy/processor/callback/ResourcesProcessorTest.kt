package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.dungeoncrusherbot.entity.Resources
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class ResourcesProcessorTest : ShouldSpec({
    val stateService = mockk<StateService>()
    val userService = mockk<UserService>()
    val processor = ResourcesProcessor(userService, stateService)

    beforeTest {
        clearMocks(stateService, userService)
    }

    should("обновлять lastServerId пользователя и возвращать FINAL") {
        val telegramUser = mockk<TgUser>(relaxed = true) {
            every { id } returns 101L
            every { firstName } returns "Tester"
        }
        val userState = UserState(userId = 101L, state = StateCode.RESOURCES)
        every { stateService.getState(telegramUser) } returns userState
        every { stateService.saveState(any()) } answers { firstArg() }

        val entityUser = User(
            userId = 101L,
            firstName = "Tester",
            lastName = "User",
            userName = "tester"
        ).apply {
            resources = Resources(user = this)
        }
        every { userService.getUser(101L) } returns entityUser
        justRun { userService.saveUser(any()) }

        val result = processor.execute(telegramUser, "RESOURCE 8")

        result shouldBe ExecuteStatus.FINAL
        entityUser.resources!!.lastServerId shouldBe 8
        userState.callbackData shouldBe "RESOURCE 8"
        verify { userService.saveUser(match { it.resources?.lastServerId == 8 }) }
        verify(exactly = 1) { stateService.saveState(userState) }
    }
})
