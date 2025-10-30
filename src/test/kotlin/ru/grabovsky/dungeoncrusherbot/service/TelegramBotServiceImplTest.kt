package ru.grabovsky.dungeoncrusherbot.service

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.entity.NotifyHistory
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.entity.UpdateMessage
import ru.grabovsky.dungeoncrusherbot.entity.User as BotUserEntity
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.NotifyHistoryService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ReleaseNoteDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.time.Instant
import java.util.Locale

class TelegramBotServiceImplTest : ShouldSpec({

    val messageServiceGenerate = mockk<MessageGenerateService>()
    val telegramClient = mockk<TelegramClient>()
    val stateService = mockk<StateService>()
    val userService = mockk<UserService>()
    val notifyHistoryService = mockk<NotifyHistoryService>()

    val service = TelegramBotServiceImpl(
        messageServiceGenerate,
        telegramClient,
        stateService,
        userService,
        notifyHistoryService,
    )

    val tgUser = mockk<User>(relaxed = true) {
        every { id } returns 111L
        every { userName } returns "tester"
        every { firstName } returns "Tester"
    }

    beforeTest {
        clearMocks(
            messageServiceGenerate,
            telegramClient,
            stateService,
            userService,
            notifyHistoryService,
            answers = true
        )
    }

    should("persist state on processState call") {
        justRun { stateService.updateState(tgUser, StateCode.WAITING) }

        service.processState(tgUser, StateCode.WAITING)

        verify { stateService.updateState(tgUser, StateCode.WAITING) }
    }

    should("return false for siege notification with empty servers") {
        val result = service.sendNotification(1L, NotificationType.SIEGE, emptyList())
        result.shouldBeFalse()
    }

    should("send siege notification and store history") {
        val servers = listOf(Server(id = 2, name = "Two"))
        val sendSlot = slot<SendMessage>()
        every { messageServiceGenerate.process(StateCode.NOTIFICATION_SIEGE, any(), any()) } returns "payload"
        every { telegramClient.execute(capture(sendSlot)) } returns mockk(relaxed = true)
        justRun { notifyHistoryService.saveHistory(any()) }

        val result = service.sendNotification(42L, NotificationType.SIEGE, servers, isBefore = true)

        result.shouldBeTrue()
        sendSlot.captured.chatId shouldBe "42"
        verify {
            messageServiceGenerate.process(
                StateCode.NOTIFICATION_SIEGE,
                ServerDto(listOf(2), true),
                any()
            )
        }
        verify { notifyHistoryService.saveHistory(any()) }
    }

    should("delete old notifications") {
        val oldInstant = Instant.now().minusSeconds(7200)
        val history = listOf(
            NotifyHistory(userId = 1L, messageId = 100, text = "a", sendTime = oldInstant),
            NotifyHistory(userId = 1L, messageId = 200, text = "b", sendTime = oldInstant)
        )
        every { notifyHistoryService.getNotDeletedHistoryEvent() } returns history
        every { telegramClient.execute(any<DeleteMessages>()) } returns true
        justRun { notifyHistoryService.markAsDeleted(history) }

        service.deleteOldNotify()

        val deleteSlot = slot<DeleteMessages>()
        verify { telegramClient.execute(capture(deleteSlot)) }
        deleteSlot.captured.chatId shouldBe "1"
        deleteSlot.captured.messageIds shouldBe listOf(100, 200)
        verify { notifyHistoryService.markAsDeleted(history) }
    }

    should("send release notes with resolved locale") {
        every { messageServiceGenerate.process(any(), any(), any()) } returns "payload"
        every { telegramClient.execute(any<SendMessage>()) } returns mockk(relaxed = true)
        val user = BotUserEntity(456L, "Tester", null, "tester", language = "ru")
        val updateMessage = UpdateMessage(version = "1.0", text = "ru", textEn = "en")

        service.sendReleaseNotes(user, updateMessage)

        val dtoSlot = slot<ReleaseNoteDto>()
        val localeSlot = slot<Locale>()
        verify { messageServiceGenerate.process(StateCode.RELEASE_NOTES, capture(dtoSlot), capture(localeSlot)) }
        dtoSlot.captured.version shouldBe "1.0"
        localeSlot.captured.language shouldBe "ru"
        verify { telegramClient.execute(any<SendMessage>()) }
    }
})
