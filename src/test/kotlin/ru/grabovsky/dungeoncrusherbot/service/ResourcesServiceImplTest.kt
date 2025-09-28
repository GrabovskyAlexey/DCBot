package ru.grabovsky.dungeoncrusherbot.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.justRun
import jakarta.persistence.EntityNotFoundException
import java.time.LocalDate
import ru.grabovsky.dungeoncrusherbot.entity.Resources
import ru.grabovsky.dungeoncrusherbot.entity.ResourcesHistory
import ru.grabovsky.dungeoncrusherbot.entity.ServerResourceData
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.entity.UserSettings
import ru.grabovsky.dungeoncrusherbot.entity.DirectionType
import ru.grabovsky.dungeoncrusherbot.entity.ResourceType
import ru.grabovsky.dungeoncrusherbot.service.interfaces.GoogleFormService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class ResourcesServiceImplTest : ShouldSpec({

    val userService = mockk<UserService>()
    val googleFormService = mockk<GoogleFormService>(relaxed = true)
    val service = ResourcesServiceImpl(userService, googleFormService)
    val telegramUser = mockk<TgUser>(relaxed = true) {
        every { id } returns 77L
        every { userName } returns "tg"
        every { firstName } returns "TG"
    }

    lateinit var entityUser: User
    lateinit var resources: Resources
    lateinit var serverData: ServerResourceData

    fun prepareUser(lastServerId: Int = 5) {
        entityUser = User(userId = 77L, firstName = "Tester", lastName = null, userName = "tester")
        resources = Resources(user = entityUser)
        entityUser.resources = resources
        resources.lastServerId = lastServerId
        serverData = ServerResourceData()
        resources.data.servers[lastServerId] = serverData
        resources.history[lastServerId] = mutableListOf()
        every { userService.getUser(any()) } returns entityUser
        justRun { userService.saveUser(any()) }
    }

    beforeTest {
        clearMocks(userService, googleFormService, answers = true)
        prepareUser()
        justRun { googleFormService.sendDraadorCount(any(), any()) }
    }

    should("throw when user is not found") {
        every { userService.getUser(any()) } returns null

        shouldThrow<EntityNotFoundException> {
            service.processResources(telegramUser, "1", StateCode.ADD_VOID)
        }
    }

    should("throw when last server id missing") {
        prepareUser(lastServerId = 5)
        resources.lastServerId = null

        shouldThrow<IllegalStateException> {
            service.processResources(telegramUser, "1", StateCode.ADD_VOID)
        }
    }

    should("increase void count and record history when adding void") {
        service.processResources(telegramUser, "3", StateCode.ADD_VOID)

        serverData.voidCount shouldBe 3
        val history = resources.history[5]!!
        history shouldHaveSize 1
        history.last().apply {
            quantity shouldBe 3
            resource shouldBe ResourceType.VOID
            type shouldBe DirectionType.ADD
        }
        verify { userService.saveUser(entityUser) }
        verify(exactly = 0) { googleFormService.sendDraadorCount(any(), any()) }
    }

    should("decrease void count and record removal") {
        serverData.voidCount = 10

        service.processResources(telegramUser, "4", StateCode.REMOVE_VOID)

        serverData.voidCount shouldBe 6
        resources.history[5]!!.last().type shouldBe DirectionType.REMOVE
    }

    should("increase cb count and track history") {
        service.processResources(telegramUser, "5", StateCode.ADD_CB)

        serverData.cbCount shouldBe 5
        resources.history[5]!!.last().apply {
            resource shouldBe ResourceType.CB
            type shouldBe DirectionType.ADD
        }
    }

    should("decrease cb count and track removal") {
        serverData.cbCount = 7

        service.processResources(telegramUser, "2", StateCode.REMOVE_CB)

        serverData.cbCount shouldBe 5
        resources.history[5]!!.last().type shouldBe DirectionType.REMOVE
    }

    should("manage draador counts for add, sell and send") {
        service.processResources(telegramUser, "4", StateCode.ADD_DRAADOR)
        serverData.draadorCount shouldBe 4
        resources.history[5]!!.last().type shouldBe DirectionType.CATCH

        service.processResources(telegramUser, "3", StateCode.SELL_DRAADOR)
        serverData.draadorCount shouldBe 1
        resources.history[5]!!.last().type shouldBe DirectionType.TRADE

        service.processResources(telegramUser, "5", StateCode.SEND_DRAADOR)
        serverData.draadorCount shouldBe 0
        serverData.balance shouldBe 5
        resources.history[5]!!.last().type shouldBe DirectionType.OUTGOING
    }

    should("avoid negative draador count when selling more than available") {
        serverData.draadorCount = 2

        service.processResources(telegramUser, "5", StateCode.SELL_DRAADOR)

        serverData.draadorCount shouldBe 0
    }

    should("populate exchange value") {
        service.processResources(telegramUser, "some", StateCode.ADD_EXCHANGE)

        serverData.exchange shouldBe "some"
    }

    should("decrease balance and transfer to main server when receiving draador") {
        resources.data.mainServerId = 1
        resources.data.servers[1] = ServerResourceData()
        resources.history[1] = mutableListOf()
        serverData.balance = 10

        service.processResources(telegramUser, "4", StateCode.RECEIVE_DRAADOR)

        serverData.balance shouldBe 6
        resources.history[5]!!.last().type shouldBe DirectionType.INCOMING
        val mainData = resources.data.servers[1]!!
        mainData.draadorCount shouldBe 4
        resources.history[1]!!.last().run {
            resource shouldBe ResourceType.DRAADOR
            type shouldBe DirectionType.INCOMING
            fromServer shouldBe 5
        }
    }

    should("limit history to 20 records") {
        val history = resources.history[5]!!
        repeat(20) { index ->
            history.add(ResourcesHistory(LocalDate.now(), ResourceType.VOID, DirectionType.ADD, index + 1))
        }

        service.processResources(telegramUser, "1", StateCode.ADD_VOID)

        history shouldHaveSize 20
    }

    should("send watermelon report when criteria are met") {
        prepareUser(lastServerId = 8)
        entityUser.settings = UserSettings(sendWatermelon = true, discordUsername = "discord", resourcesCb = false, resourcesQuickChange = false)

        service.processResources(telegramUser, "9", StateCode.ADD_DRAADOR)

        verify { googleFormService.sendDraadorCount("9", "discord") }
    }

    should("skip watermelon report when criteria are not met") {
        prepareUser(lastServerId = 8)
        entityUser.settings = UserSettings(sendWatermelon = false, discordUsername = "discord")

        service.processResources(telegramUser, "2", StateCode.ADD_DRAADOR)

        verify(exactly = 0) { googleFormService.sendDraadorCount(any(), any()) }
    }
})






