package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.dungeoncrusherbot.entity.Resources
import ru.grabovsky.dungeoncrusherbot.entity.ServerResourceData
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.entity.UserSettings
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ResourcesService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class QuickResourceProcessorTest : ShouldSpec({
    val userService = mockk<UserService>()
    val resourcesService = mockk<ResourcesService>()
    val stateService = mockk<StateService>()
    val processor = IncrementCbProcessor(userService, resourcesService, stateService)

    beforeTest {
        clearMocks(userService, resourcesService, stateService)
    }

    fun telegramUser(): TgUser = mockk(relaxed = true) {
        every { id } returns 300L
        every { firstName } returns "Quick"
    }

    fun userEntity(configure: User.() -> Unit = {}): User {
        return User(
            userId = 300L,
            firstName = "Quick",
            lastName = "User",
            userName = "quick"
        ).apply {
            resources = Resources(user = this).apply {
                lastServerId = 7
                data.servers[7] = ServerResourceData()
            }
            configure()
        }
    }

    should("возвращать NOTHING если быстрые изменения отключены") {
        val tgUser = telegramUser()
        val state = UserState(userId = 300L, state = StateCode.SERVER_RESOURCE)
        every { stateService.getState(tgUser) } returns state
        every { stateService.saveState(any()) } answers { firstArg() }

        val entity = userEntity {
            settings = UserSettings(resourcesQuickChange = false)
        }
        every { userService.getUser(300L) } returns entity

        val result = processor.execute(tgUser, "INCREMENT_CB")

        result shouldBe ExecuteStatus.NOTHING
        state.callbackData shouldBe "INCREMENT_CB"
        verify(exactly = 1) { stateService.saveState(state) }
        verify(exactly = 0) { resourcesService.processResources(any(), any(), any()) }
    }

    should("возвращать NOTHING если CB не разрешён") {
        val tgUser = telegramUser()
        val state = UserState(userId = 300L, state = StateCode.SERVER_RESOURCE)
        every { stateService.getState(tgUser) } returns state
        every { stateService.saveState(any()) } answers { firstArg() }

        val entity = userEntity {
            settings = UserSettings(resourcesQuickChange = true, resourcesCb = false)
        }
        every { userService.getUser(300L) } returns entity

        val result = processor.execute(tgUser, "INCREMENT_CB")

        result shouldBe ExecuteStatus.NOTHING
        state.callbackData shouldBe "INCREMENT_CB"
        verify(exactly = 1) { stateService.saveState(state) }
        verify(exactly = 0) { resourcesService.processResources(any(), any(), any()) }
    }

    should("обрабатывать запрос когда быстрый режим и CB включены") {
        val tgUser = telegramUser()
        val state = UserState(userId = 300L, state = StateCode.SERVER_RESOURCE)
        every { stateService.getState(tgUser) } returnsMany listOf(state, state)
        every { stateService.saveState(any()) } answers { firstArg() }

        val entity = userEntity {
            settings = UserSettings(resourcesQuickChange = true, resourcesCb = true)
        }
        every { userService.getUser(300L) } returns entity
        justRun { resourcesService.processResources(tgUser, "1", StateCode.ADD_CB) }

        val result = processor.execute(tgUser, "INCREMENT_CB")

        result shouldBe ExecuteStatus.FINAL
        state.callbackData shouldBe "INCREMENT_CB"
        state.prevState shouldBe StateCode.UPDATE_SERVER_RESOURCE
        verify(exactly = 1) { resourcesService.processResources(tgUser, "1", StateCode.ADD_CB) }
        verify(exactly = 2) { stateService.saveState(state) }
    }
})
