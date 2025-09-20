package ru.grabovsky.dungeoncrusherbot.strategy.message.subscribe

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class SubscribeMessageTest : ShouldSpec({
    val messageService = mockk<MessageGenerateService>(relaxed = true)
    val userService = mockk<UserService>()
    val serverService = mockk<ServerService>()
    val message = SubscribeMessage(messageService, userService, serverService)

    should("помечать подписанные сервера и переключать действия") {
        val tgUser = mockk<TgUser>(relaxed = true) { every { id } returns 700L }
        val servers = (1..4).map { Server(id = it, name = "Server $it") }
        every { serverService.getAllServers() } returns servers
        every { userService.getUser(700L) } returns User(700L, "Tester", null, "tester").apply {
            this.servers.addAll(listOf(servers[1], servers[3]))
        }

        val buttons = message.inlineButtons(tgUser, null)

        buttons.shouldHaveSize(4)
        buttons[1].data shouldBe CallbackObject(StateCode.SUBSCRIBE, "UNSUBSCRIBE 2")
        buttons[0].data shouldBe CallbackObject(StateCode.SUBSCRIBE, "SUBSCRIBE 1")
        buttons[3].data shouldBe CallbackObject(StateCode.SUBSCRIBE, "UNSUBSCRIBE 4")
    }
})
