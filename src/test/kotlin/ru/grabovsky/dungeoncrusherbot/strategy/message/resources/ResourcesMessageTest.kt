package ru.grabovsky.dungeoncrusherbot.strategy.message.resources

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ResourceDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerResourceDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.util.Locale
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class ResourcesMessageTest : ShouldSpec({
    val messageService = mockk<MessageGenerateService>(relaxed = true)
    val serverService = mockk<ServerService>()
    val message = ResourcesMessage(messageService, serverService)

    should("раскладывать серверы по рядам и помечать главный") {
        val servers = (1..6).map { id -> Server(id = id, name = "Server $id") }
        every { serverService.getAllServers() } returns servers

        val dto = ResourceDto(
            servers = listOf(
                ServerResourceDto(
                    id = 2,
                    main = true,
                    cbEnabled = true,
                    quickResourceEnabled = true,
                    cbCount = 0,
                    hasMain = true
                )
            )
        )
        val user = mockk<TgUser>(relaxed = true)

        val buttons = message.inlineButtons(user, dto, Locale.forLanguageTag("ru"))

        buttons.shouldHaveSize(6)
        buttons.take(5).all { it.rowPos == 1 } shouldBe true
        buttons.last().rowPos shouldBe 2
        buttons.first { it.data == CallbackObject(StateCode.RESOURCES, "RESOURCE 2") }.text shouldBe "\uD83D\uDC512"
        buttons.all { it.data.state == StateCode.RESOURCES } shouldBe true
        buttons.map { it.text }.toSet() shouldBe setOf("1", "\uD83D\uDC512", "3", "4", "5", "6")
    }
})
