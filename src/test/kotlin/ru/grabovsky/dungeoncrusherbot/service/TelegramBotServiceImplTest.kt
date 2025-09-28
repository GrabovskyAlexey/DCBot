package ru.grabovsky.dungeoncrusherbot.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.dungeoncrusherbot.dto.MessageModelDto
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.entity.NotifyHistory
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.entity.Siege
import ru.grabovsky.dungeoncrusherbot.entity.UpdateMessage
import ru.grabovsky.dungeoncrusherbot.entity.User as BotUserEntity
import ru.grabovsky.dungeoncrusherbot.event.TelegramStateEvent
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.NotifyHistoryService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.VerificationService
import ru.grabovsky.dungeoncrusherbot.strategy.context.MessageContext
import ru.grabovsky.dungeoncrusherbot.strategy.context.StateContext
import ru.grabovsky.dungeoncrusherbot.strategy.dto.AdminMessageDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.DataModel
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ReleaseNoteDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.time.Instant
import java.time.LocalTime
import java.util.Locale

class TelegramBotServiceImplTest : ShouldSpec({

    val messageServiceGenerate = mockk<MessageGenerateService>()
    val telegramClient = mockk<TelegramClient>()
    val stateContext = mockk<StateContext>()
    val messageContext = mockk<MessageContext<DataModel>>()
    val publisher = mockk<ApplicationEventPublisher>(relaxed = true)
    val stateService = mockk<StateService>()
    val userService = mockk<UserService>()
    val notifyHistoryService = mockk<NotifyHistoryService>()
    val verificationService = mockk<VerificationService>()
    val objectMapper = ObjectMapper()

    val service = TelegramBotServiceImpl(
        messageServiceGenerate,
        telegramClient,
        stateContext,
        messageContext,
        publisher,
        stateService,
        userService,
        notifyHistoryService,
        verificationService,
        objectMapper
    )

    val tgUser = mockk<User>(relaxed = true) {
        every { id } returns 111L
        every { userName } returns "tester"
        every { firstName } returns "Tester"
    }

    val defaultMessageModel = MessageModelDto(
        message = "hello",
        inlineButtons = emptyList(),
        replyButtons = emptyList()
    )

    fun telegramMessage(id: Int, body: String = "payload"): Message = mockk {
        every { messageId } returns id
        every { text } returns body
    }

    beforeTest {
        clearMocks(
            messageServiceGenerate,
            telegramClient,
            stateContext,
            messageContext,
            publisher,
            stateService,
            userService,
            notifyHistoryService,
            verificationService,
            answers = true
        )
        every { messageContext.getMessage(any(), any(), any()) } returns defaultMessageModel
        every { userService.getUser(any()) } returns BotUserEntity(
            userId = 111L,
            firstName = "Tester",
            lastName = null,
            userName = "tester",
            language = "ru"
        )
        every { telegramClient.execute(any<BotApiMethod<*>>()) } answers {
            when (val method = firstArg<BotApiMethod<*>>()) {
                is SendMessage -> telegramMessage(100)
                is EditMessageText -> telegramMessage(200)
                is DeleteMessages -> true
                else -> true
            }
        }
        every { stateService.saveState(any()) } answers { firstArg() }
        every { stateContext.next(any(), any()) } returns null
        every { messageServiceGenerate.process(any(), any(), any()) } returns "generated"
        justRun { publisher.publishEvent(any()) }
        justRun { notifyHistoryService.saveHistory(any()) }
        justRun { notifyHistoryService.markAsDeleted(any()) }
        justRun { verificationService.verify(any(), any()) }
    }

    should("add sent message id to delete list for delete mark states") {
        val state = UserStateFactory.state(StateCode.ADD_NOTE)
        every { stateService.getState(tgUser) } returns state

        val message = telegramMessage(42)
        every { telegramClient.execute(any<SendMessage>()) } returns message

        service.processState(tgUser, StateCode.ADD_NOTE)

        state.deletedMessages shouldContainExactly listOf(42)
        verify { stateService.saveState(state) }
        verify(exactly = 0) { stateContext.next(any(), any()) }
    }

    should("publish next state when pause is disabled") {
        val state = UserStateFactory.state(StateCode.START)
        every { stateService.getState(tgUser) } returns state
        every { stateContext.next(tgUser, StateCode.START) } returns StateCode.WAITING

        service.processState(tgUser, StateCode.START)

        verify { stateContext.next(tgUser, StateCode.START) }
        val eventSlot = slot<TelegramStateEvent>()
        verify { publisher.publishEvent(capture(eventSlot)) }
        eventSlot.captured.user shouldBe tgUser
        eventSlot.captured.stateCode shouldBe StateCode.WAITING
    }

    should("delegate verification action to verification service") {
        service.processState(tgUser, StateCode.VERIFY)

        verify { verificationService.verify(tgUser, StateCode.VERIFY) }
    }

    should("edit message using linked message id and persist state") {
        val state = UserStateFactory.state(StateCode.UPDATE_RESOURCES).apply {
            updateMessageByState[StateCode.RESOURCES] = 13
        }
        every { stateService.getState(tgUser) } returns state

        service.processState(tgUser, StateCode.UPDATE_RESOURCES)

        val editSlot = slot<EditMessageText>()
        verify { telegramClient.execute(capture(editSlot)) }
        editSlot.captured.messageId shouldBe 13
        verify { stateService.saveState(state) }
    }

    should("delete collected messages and clear state") {
        val state = UserStateFactory.state(StateCode.VERIFICATION_SUCCESS).apply {
            deletedMessages.addAll(listOf(7, 8))
        }
        every { stateService.getState(tgUser) } returns state

        service.processState(tgUser, StateCode.VERIFICATION_SUCCESS)

        val deleteSlot = slot<DeleteMessages>()
        verify { telegramClient.execute(capture(deleteSlot)) }
        deleteSlot.captured.messageIds shouldBe listOf(7, 8)
        state.deletedMessages shouldBe emptyList<Int>()
        verify { stateService.saveState(state) }
    }

    should("persist notification history on successful send") {
        val server = Server(1, "server", mutableSetOf(Siege(1, LocalTime.NOON)))
        val message = telegramMessage(55, "notify")
        every { telegramClient.execute(any<SendMessage>()) } returns message

        val historySlot = slot<NotifyHistory>()
        every { notifyHistoryService.saveHistory(capture(historySlot)) } answers { }

        val result = service.sendNotification(999, NotificationType.SIEGE, listOf(server), false)

        result.shouldBeTrue()
        historySlot.captured.userId shouldBe 999
        historySlot.captured.messageId shouldBe 55
        historySlot.captured.text shouldBe "notify"
    }

    should("return false for forbidden notification errors") {
        val exception = mockk<TelegramApiRequestException>(relaxed = true) {
            every { errorCode } returns 403
        }
        every { telegramClient.execute(any<SendMessage>()) } throws exception

        val result = service.sendNotification(1000, NotificationType.SIEGE, listOf(Server(1, "s", mutableSetOf())), false)

        result.shouldBeFalse()
        verify(exactly = 0) { notifyHistoryService.saveHistory(any()) }
    }

    should("remove old notifications and mark entries as deleted") {
        val oldInstant = Instant.now().minusSeconds(3 * 3600)
        val history = listOf(
            NotifyHistory(userId = 777, messageId = 1, text = "old", sendTime = oldInstant),
            NotifyHistory(userId = 777, messageId = 2, text = "old2", sendTime = oldInstant)
        )
        every { notifyHistoryService.getNotDeletedHistoryEvent() } returns history

        service.deleteOldNotify()

        val deleteHistorySlot = slot<DeleteMessages>()
        verify { telegramClient.execute(capture(deleteHistorySlot)) }
        deleteHistorySlot.captured.chatId shouldBe "777"
        deleteHistorySlot.captured.messageIds shouldBe listOf(1, 2)
        verify {
            notifyHistoryService.markAsDeleted(match { records ->
                records.map { it.messageId } == listOf(1, 2)
            })
        }
    }

    should("send release notes with generated message in russian by default") {
        val updateMessage = UpdateMessage(version = "1.0", text = "ru", textEn = "en")
        val sent = telegramMessage(123)
        val user = BotUserEntity(456L, "Tester", null, "tester", language = "ru")
        every { telegramClient.execute(any<SendMessage>()) } returns sent

        service.sendReleaseNotes(user, updateMessage)

        val dtoSlot = slot<ReleaseNoteDto>()
        val localeSlot = slot<Locale>()
        verify {
            messageServiceGenerate.process(
                StateCode.RELEASE_NOTES,
                capture(dtoSlot),
                capture(localeSlot)
            )
        }
        dtoSlot.captured.version shouldBe "1.0"
        dtoSlot.captured.text shouldBe "ru"
        dtoSlot.captured.textEn shouldBe "en"
        localeSlot.captured.language shouldBe "ru"
        val sendSlot = slot<SendMessage>()
        verify { telegramClient.execute(capture(sendSlot)) }
        sendSlot.captured.chatId shouldBe "456"
    }

    should("send release notes in english for non russian language") {
        val updateMessage = UpdateMessage(version = "1.0", text = "ru", textEn = "en")
        val sent = telegramMessage(321)
        val user = BotUserEntity(789L, "Tester", null, "tester", language = "en")
        every { telegramClient.execute(any<SendMessage>()) } returns sent

        service.sendReleaseNotes(user, updateMessage)

        val dtoSlot = slot<ReleaseNoteDto>()
        val localeSlot = slot<Locale>()
        verify {
            messageServiceGenerate.process(
                StateCode.RELEASE_NOTES,
                capture(dtoSlot),
                capture(localeSlot)
            )
        }
        dtoSlot.captured.version shouldBe "1.0"
        dtoSlot.captured.text shouldBe "ru"
        dtoSlot.captured.textEn shouldBe "en"
        localeSlot.captured.language shouldBe "en"
        val sendSlot = slot<SendMessage>()
        verify { telegramClient.execute(capture(sendSlot)) }
        sendSlot.captured.chatId shouldBe "789"
    }

    should("fallback to russian release notes when english text is missing") {
        val updateMessage = UpdateMessage(version = "1.0", text = "ru")
        val sent = telegramMessage(654)
        val user = BotUserEntity(987L, "Tester", null, "tester", language = "en")
        every { telegramClient.execute(any<SendMessage>()) } returns sent

        service.sendReleaseNotes(user, updateMessage)

        val dtoSlot = slot<ReleaseNoteDto>()
        val localeSlot = slot<Locale>()
        verify {
            messageServiceGenerate.process(
                StateCode.RELEASE_NOTES,
                capture(dtoSlot),
                capture(localeSlot)
            )
        }
        dtoSlot.captured.version shouldBe "1.0"
        dtoSlot.captured.text shouldBe "ru"
        dtoSlot.captured.textEn shouldBe null
        localeSlot.captured.language shouldBe "en"
        val sendSlot = slot<SendMessage>()
        verify { telegramClient.execute(capture(sendSlot)) }
        sendSlot.captured.chatId shouldBe "987"
    }

    should("send admin message with generated template") {
        val dto = AdminMessageDto("a", "b", 1L, "body")
        val sent = telegramMessage(321)
        every { telegramClient.execute(any<SendMessage>()) } returns sent

        service.sendAdminMessage(654, dto)

        verify { messageServiceGenerate.process(StateCode.ADMIN_MESSAGE, dto, any()) }
        val sendSlot = slot<SendMessage>()
        verify { telegramClient.execute(capture(sendSlot)) }
        sendSlot.captured.chatId shouldBe "654"
    }
})

private object UserStateFactory {
    fun state(stateCode: StateCode) = ru.grabovsky.dungeoncrusherbot.entity.UserState(
        userId = 111L,
        state = stateCode
    )
}








