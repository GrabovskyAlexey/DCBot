package ru.grabovsky.dungeoncrusherbot.strategy.data.resources

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.entity.Resources
import ru.grabovsky.dungeoncrusherbot.entity.ServerResourceData
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.entity.UserSettings
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class ResourcesDataRepositoryTest : ShouldSpec({
    val userService = mockk<UserService>()
    val repository = ResourcesDataRepository(userService)

    should("возвращать сервера с данными и текущий главный сервер") {
        val entityUser = User(
            userId = 400L,
            firstName = "Data",
            lastName = null,
            userName = "data"
        ).apply {
            settings = UserSettings(resourcesCb = true, resourcesQuickChange = true)
            resources = Resources(user = this).apply {
                data.servers[1] = ServerResourceData(draadorCount = 5)
                data.servers[2] = ServerResourceData()
                data.servers[3] = ServerResourceData(balance = 0)
                data.mainServerId = 2
            }
        }
        every { userService.getUser(400L) } returns entityUser
        val tgUser = mockk<TgUser>(relaxed = true) {
            every { id } returns 400L
        }

        val dto = repository.getData(tgUser)

        dto.servers.shouldHaveSize(2)
        val mainServer = dto.servers.first { it.id == 2 }
        mainServer.main shouldBe true
        mainServer.cbEnabled shouldBe true
        mainServer.quickResourceEnabled shouldBe true
        val dataServer = dto.servers.first { it.id == 1 }
        dataServer.draadorCount shouldBe 5
        dataServer.main shouldBe false
    }
})
