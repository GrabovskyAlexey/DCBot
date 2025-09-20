package ru.grabovsky.dungeoncrusherbot.strategy.data.resources

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.dungeoncrusherbot.entity.DirectionType
import ru.grabovsky.dungeoncrusherbot.entity.Resources
import ru.grabovsky.dungeoncrusherbot.entity.ResourcesHistory
import ru.grabovsky.dungeoncrusherbot.entity.ServerResourceData
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.entity.UserSettings
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.entity.ResourceType
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.time.LocalDate
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class ServerResourceDataRepositoryTest : ShouldSpec({
    val userService = mockk<UserService>()
    val stateService = mockk<StateService>()
    val repository = ServerResourceDataRepository(userService, stateService)

    should("return history and notes for the last selected server") {
        val entityUser = User(
            userId = 500L,
            firstName = "Server",
            lastName = null,
            userName = "server"
        ).apply {
            settings = UserSettings(resourcesCb = true, resourcesQuickChange = true)
            resources = Resources(user = this).apply {
                lastServerId = 8
                data.mainServerId = 8
                data.servers[8] = ServerResourceData(
                    exchange = "10",
                    draadorCount = 3,
                    voidCount = 1,
                    balance = 2,
                    notifyDisable = true,
                    cbCount = 4,
                )
                history[8] = mutableListOf(
                    ResourcesHistory(LocalDate.of(2024, 9, 18), ResourceType.DRAADOR, DirectionType.ADD, 5)
                )
            }
            notes.addAll(listOf("one", "two"))
        }
        val tgUser = mockk<TgUser>(relaxed = true) { every { id } returns 500L }
        val state = UserState(userId = 500L, state = StateCode.SERVER_RESOURCE, callbackData = "RESOURCE_HISTORY")
        every { stateService.getState(tgUser) } returns state
        justRun { stateService.saveState(any()) }
        justRun { stateService.updateState(tgUser, any()) }
        every { userService.getUser(500L) } returns entityUser
        justRun { userService.saveUser(entityUser) }

        val dto = repository.getData(tgUser)

        dto.id shouldBe 8
        dto.main shouldBe true
        dto.history shouldContainExactly entityUser.resources!!.history[8]!!.map { it.toString() }
        dto.notes shouldContainExactly entityUser.notes
        dto.cbEnabled shouldBe true
        dto.quickResourceEnabled shouldBe true
        verify { stateService.updateState(tgUser, StateCode.SERVER_RESOURCE) }
        verify { userService.saveUser(entityUser) }
    }
})
