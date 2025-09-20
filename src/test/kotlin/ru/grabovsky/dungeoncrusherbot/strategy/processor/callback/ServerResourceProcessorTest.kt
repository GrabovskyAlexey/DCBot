package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
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
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class ServerResourceProcessorTest : ShouldSpec({
    val stateService = mockk<StateService>()
    val userService = mockk<UserService>()
    val processor = ServerResourceProcessor(userService, stateService)

    beforeTest {
        clearMocks(stateService, userService)
    }

    fun buildUser(): User {
        return User(
            userId = 200L,
            firstName = "Tester",
            lastName = "User",
            userName = "tester"
        ).apply {
            resources = Resources(user = this).apply {
                lastServerId = 8
                data.servers[8] = ServerResourceData(exchange = "123", notifyDisable = false)
            }
            settings = UserSettings(resourcesCb = true, resourcesQuickChange = true)
        }
    }

    fun mockState(user: TgUser): UserState {
        val state = UserState(userId = user.id, state = StateCode.SERVER_RESOURCE)
        every { stateService.getState(user) } returnsMany listOf(state, state)
        every { stateService.saveState(any()) } answers { firstArg() }
        return state
    }

    fun mockUserService(user: User) {
        every { userService.getUser(user.userId) } returns user
        justRun { userService.saveUser(user) }
    }

    should("удалять обмен и помечать предыдущее состояние") {
        val telegramUser = mockk<TgUser>(relaxed = true) {
            every { id } returns 200L
            every { firstName } returns "Tester"
        }
        val state = mockState(telegramUser)
        val entityUser = buildUser()
        mockUserService(entityUser)

        val result = processor.execute(telegramUser, "REMOVE_EXCHANGE")

        result shouldBe ExecuteStatus.FINAL
        entityUser.resources!!.data.servers[8]!!.exchange shouldBe null
        state.prevState shouldBe StateCode.UPDATE_SERVER_RESOURCE
        state.callbackData shouldBe "REMOVE_EXCHANGE"
        verify { userService.saveUser(entityUser) }
        verify(exactly = 2) { stateService.saveState(state) }
    }

    should("переключать флаг уведомлений по требованию") {
        val telegramUser = mockk<TgUser>(relaxed = true) {
            every { id } returns 200L
            every { firstName } returns "Tester"
        }
        val state = mockState(telegramUser)
        val entityUser = buildUser()
        mockUserService(entityUser)

        val result = processor.execute(telegramUser, "DISABLE_NOTIFY")

        result shouldBe ExecuteStatus.FINAL
        entityUser.resources!!.data.servers[8]!!.notifyDisable.shouldBeTrue()
        state.prevState shouldBe StateCode.UPDATE_SERVER_RESOURCE
        verify { userService.saveUser(entityUser) }
        verify(exactly = 2) { stateService.saveState(state) }
    }

    should("назначать главный сервер при соответствующем запросе") {
        val telegramUser = mockk<TgUser>(relaxed = true) {
            every { id } returns 200L
            every { firstName } returns "Tester"
        }
        val state = mockState(telegramUser)
        val entityUser = buildUser().apply {
            resources!!.data.mainServerId = null
        }
        mockUserService(entityUser)

        val result = processor.execute(telegramUser, "SET_MAIN")

        result shouldBe ExecuteStatus.FINAL
        entityUser.resources!!.data.mainServerId shouldBe 8
        state.prevState shouldBe StateCode.UPDATE_SERVER_RESOURCE
        verify { userService.saveUser(entityUser) }
        verify(exactly = 2) { stateService.saveState(state) }
    }

    should("сбрасывать главный сервер при запросе REMOVE_MAIN") {
        val telegramUser = mockk<TgUser>(relaxed = true) {
            every { id } returns 200L
            every { firstName } returns "Tester"
        }
        val state = mockState(telegramUser)
        val entityUser = buildUser().apply {
            resources!!.data.mainServerId = 8
        }
        mockUserService(entityUser)

        val result = processor.execute(telegramUser, "REMOVE_MAIN")

        result shouldBe ExecuteStatus.FINAL
        entityUser.resources!!.data.mainServerId shouldBe null
        state.prevState shouldBe StateCode.UPDATE_SERVER_RESOURCE
        verify { userService.saveUser(entityUser) }
        verify(exactly = 2) { stateService.saveState(state) }
    }

    should("переключать уведомления обратно при повторном запросе") {
        val telegramUser = mockk<TgUser>(relaxed = true) {
            every { id } returns 200L
            every { firstName } returns "Tester"
        }
        val state = mockState(telegramUser)
        val entityUser = buildUser().apply {
            resources!!.data.servers[8]!!.notifyDisable = true
        }
        mockUserService(entityUser)

        val result = processor.execute(telegramUser, "DISABLE_NOTIFY")

        result shouldBe ExecuteStatus.FINAL
        entityUser.resources!!.data.servers[8]!!.notifyDisable.shouldBeFalse()
        state.prevState shouldBe StateCode.UPDATE_SERVER_RESOURCE
        verify { userService.saveUser(entityUser) }
        verify(exactly = 2) { stateService.saveState(state) }
    }
})
