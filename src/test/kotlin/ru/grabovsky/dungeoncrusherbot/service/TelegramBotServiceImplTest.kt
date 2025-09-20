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
import ru.grabovsky.dungeoncrusherbot.event.TelegramStateEvent
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.NotifyHistoryService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.VerificationService
import ru.grabovsky.dungeoncrusherbot.strategy.context.MessageContext
import ru.grabovsky.dungeoncrusherbot.strategy.context.StateContext
import ru.grabovsky.dungeoncrusherbot.strategy.dto.AdminMessageDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.DataModel
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.time.Instant
import java.time.LocalTime

class TelegramBotServiceImplTest : ShouldSpec({

    val messageServiceGenerate = mockk<MessageGenerateService>()
    val telegramClient = mockk<TelegramClient>()
    val stateContext = mockk<StateContext>()
    val messageContext = mockk<MessageContext<DataModel>>()
    val publisher = mockk<ApplicationEventPublisher>(relaxed = true)
    val stateService = mockk<StateService>()
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
            notifyHistoryService,
            verificationService,
            answers = true
        )
        every { messageContext.getMessage(any(), any()) } returns defaultMessageModel
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
        every { messageServiceGenerate.process(any(), any()) } returns "generated"
        justRun { publisher.publishEvent(any()) }
        justRun { notifyHistoryService.saveHistory(any()) }
        justRun { notifyHistoryService.markAsDeleted(any()) }
        justRun { verificationService.verify(any(), any()) }
    }

    should("add sent message id to delete list for delete mark states") {
        val state = UserStateFactory.state(StateCode.ADD_NOTE)
        every { stateService.getState(tgUser) } returns state

        val message = telegramMessage(42)
        every { telegramClient.execute(match<BotApiMethod<*>> { it is SendMessage }) } returns message

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
        verify {
            publisher.publishEvent(match<TelegramStateEvent> {
                it.user == tgUser && it.stateCode == StateCode.WAITING
            })
        }
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

        verify { telegramClient.execute(match<BotApiMethod<*>> { it is EditMessageText && (it as EditMessageText).messageId == 13 }) }
        verify { stateService.saveState(state) }
    }

    should("delete collected messages and clear state") {
        val state = UserStateFactory.state(StateCode.VERIFICATION_SUCCESS).apply {
            deletedMessages.addAll(listOf(7, 8))
        }
        every { stateService.getState(tgUser) } returns state

        service.processState(tgUser, StateCode.VERIFICATION_SUCCESS)

        verify { telegramClient.execute(match<BotApiMethod<*>> { it is DeleteMessages && (it as DeleteMessages).messageIds == listOf(7, 8) }) }
        state.deletedMessages shouldBe emptyList<Int>()
        verify { stateService.saveState(state) }
    }

    should("persist notification history on successful send") {
        val server = Server(1, "server", mutableSetOf(Siege(1, LocalTime.NOON)))
        val message = telegramMessage(55, "notify")
        every { telegramClient.execute(match<BotApiMethod<*>> { it is SendMessage }) } returns message

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
        every { telegramClient.execute(match<BotApiMethod<*>> { it is SendMessage }) } throws exception

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

        verify {
            telegramClient.execute(match<BotApiMethod<*>> {
                it is DeleteMessages && it.chatId == "777" && it.messageIds == listOf(1, 2)
            })
        }
        verify {
            notifyHistoryService.markAsDeleted(match { records ->
                records.map { it.messageId } == listOf(1, 2)
            })
        }
    }

    should("send release notes with generated message") {
        val updateMessage = UpdateMessage(version = "1.0", text = "notes")
        val sent = telegramMessage(123)
        every { telegramClient.execute(match<BotApiMethod<*>> { it is SendMessage }) } returns sent

        service.sendReleaseNotes(456, updateMessage)

        verify { messageServiceGenerate.process(StateCode.RELEASE_NOTES, updateMessage) }
        verify {
            telegramClient.execute(match<BotApiMethod<*>> { it is SendMessage && (it as SendMessage).chatId == "456" })
        }
    }

    should("send admin message with generated template") {
        val dto = AdminMessageDto("a", "b", 1L, "body")
        val sent = telegramMessage(321)
        every { telegramClient.execute(match<BotApiMethod<*>> { it is SendMessage }) } returns sent

        service.sendAdminMessage(654, dto)

        verify { messageServiceGenerate.process(StateCode.ADMIN_MESSAGE, dto) }
        verify {
            telegramClient.execute(match<BotApiMethod<*>> { it is SendMessage && (it as SendMessage).chatId == "654" })
        }
    }
})

private object UserStateFactory {
    fun state(stateCode: StateCode) = ru.grabovsky.dungeoncrusherbot.entity.UserState(
        userId = 111L,
        state = stateCode
    )
}








