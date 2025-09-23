package ru.grabovsky.dungeoncrusherbot.strategy.message

import io.kotest.core.spec.style.ShouldSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.strategy.dto.DataModel
import ru.grabovsky.dungeoncrusherbot.strategy.dto.MazeDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.NotesDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ResourceDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerResourceDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.SettingsDto
import ru.grabovsky.dungeoncrusherbot.strategy.message.maze.MazeMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.maze.UpdateMazeMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.notes.RemoveNoteMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.notes.UpdateNotesMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.resources.AddCbMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.resources.AddDraadorMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.resources.AddExchangeMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.resources.AddNoteMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.resources.AddVoidMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.resources.ReceiveDraadorMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.resources.RemoveCbMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.resources.RemoveVoidMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.resources.ResourcesMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.resources.SellDraadorMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.resources.SendDraadorMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.resources.ServerResourceMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.resources.UpdateResourcesMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.resources.UpdateServerResourceMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.settings.UpdateSettingsMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.subscribe.SubscribeMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.subscribe.UpdateSubscribeMessage
import java.util.Locale

private object EmptyData : DataModel

class MessageDelegationTest : ShouldSpec({
    val messageService = mockk<MessageGenerateService>(relaxed = true)
    val serverService = mockk<ServerService>(relaxed = true)
    val userService = mockk<UserService>(relaxed = true)

    fun <T : DataModel> assertDelegation(message: AbstractSendMessage<T>, data: T) {
        val user = mockk<User>(relaxed = true)
        message.message(user, Locale.forLanguageTag("ru"), data)
        verify { messageService.process(message.classStateCode(), data, any()) }
    }

    beforeTest {
        clearMocks(messageService)
        every { messageService.process(any(), any(), any()) } returns "generated"
    }

    should("делегировать генерацию шаблонов для базовых ресурсных сообщений") {
        listOf(
            AddCbMessage(messageService),
            AddDraadorMessage(messageService),
            AddExchangeMessage(messageService),
            AddNoteMessage(messageService),
            AddVoidMessage(messageService),
            ReceiveDraadorMessage(messageService),
            RemoveCbMessage(messageService),
            RemoveVoidMessage(messageService),
            SellDraadorMessage(messageService),
            SendDraadorMessage(messageService),
            RemoveNoteMessage(messageService)
        ).forEach { msg ->
            assertDelegation(msg, EmptyData)
        }
    }

    should("делегировать генерацию для ResourcesMessage и производных") {
        val resourceDto = ResourceDto()
        val serverDto = ServerResourceDto(
            id = 1,
            draadorCount = 0,
            voidCount = 0,
            balance = 0,
            exchange = null,
            history = null,
            hasHistory = false,
            notifyDisable = false,
            main = false,
            cbEnabled = false,
            quickResourceEnabled = false,
            cbCount = 0,
            notes = emptyList(),
            hasMain = false,
        )
        assertDelegation(ResourcesMessage(messageService, serverService), resourceDto)
        assertDelegation(ServerResourceMessage(messageService), serverDto)
        assertDelegation(UpdateResourcesMessage(messageService, serverService), resourceDto)
        assertDelegation(UpdateServerResourceMessage(messageService, serverService), serverDto)
    }

    should("делегировать генерацию для сообщений лабиринта") {
        val dto = MazeDto(sameSteps = false)
        assertDelegation(MazeMessage(messageService), dto)
        assertDelegation(UpdateMazeMessage(messageService), dto)
    }

    should("делегировать генерацию для сообщений подписки") {
        val dto = ServerDto(listOf(1, 2))
        assertDelegation(SubscribeMessage(messageService, userService, serverService), dto)
        assertDelegation(UpdateSubscribeMessage(messageService, userService, serverService), dto)
    }

    should("делегировать генерацию для обновлений заметок и настроек") {
        assertDelegation(UpdateNotesMessage(messageService), NotesDto(emptyList()))
        assertDelegation(UpdateSettingsMessage(messageService), SettingsDto(false, false, false, false))
    }
})
