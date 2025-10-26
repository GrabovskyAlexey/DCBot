package ru.grabovsky.dungeoncrusherbot.strategy.flow.notes

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowCallbackContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowMessageContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowResult
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStartContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.SendMessageAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStateHolder
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import java.util.Locale
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class NotesFlowTest : ShouldSpec({

    val userService = mockk<UserService>()
    val viewService = mockk<NotesViewService>()
    val promptBuilder = mockk<NotesPromptBuilder>()
    val i18nService = mockk<I18nService>()
    val flow = NotesFlow(userService, viewService, promptBuilder, i18nService)

    val telegramUser = mockk<TgUser>(relaxed = true) {
        every { id } returns 101L
        every { firstName } returns "Tester"
    }
    val locale = Locale.ENGLISH

    val overview = NotesOverviewModel(
        notes = listOf(NoteItem(1, "First")),
        buttons = listOf(NoteButton(label = "Add", action = "ADD", row = 0, col = 0))
    )

    beforeTest {
        clearMocks(userService, viewService, promptBuilder, i18nService, answers = true)
        every { viewService.buildOverview(any(), any()) } returns overview
        every { promptBuilder.addPrompt(any(), invalid = any()) } returns NotesPromptModel("enter text")
        every { promptBuilder.removePrompt(any(), any(), invalid = any()) } returns NotesPromptModel("remove", listOf("1. note"))
        every { userService.addNote(any(), any()) } returns true
        every { userService.removeNote(any(), any()) } returns true
        every { userService.getUser(any()) } returns null
        every { i18nService.i18n(any(), any(), any(), *anyVararg()) } returns "Cancel"
        justRun { userService.clearNotes(any()) }
        justRun { userService.saveUser(any()) }
    }

    fun FlowResult<NotesFlowState>.shouldHaveMainStep() {
        stepKey shouldBe NotesStep.MAIN.key
        actions.shouldHaveSize(1)
    }

    should("build overview on start") {
        val result = flow.start(FlowStartContext(telegramUser, locale))

        result.shouldHaveMainStep()
        verify { viewService.buildOverview(telegramUser, locale) }
    }

    should("prompt for adding note and handle message") {
        val stateHolder = FlowStateHolder(
            stepKey = NotesStep.MAIN.key,
            payload = NotesFlowState(),
            messageBindings = mapOf("notes_main_message" to 10)
        )
        val callbackContext = FlowCallbackContext(
            user = telegramUser,
            locale = locale,
            state = stateHolder
        )
        val callback = mockk<CallbackQuery>(relaxed = true) { every { id } returns "cb" }

        val promptResult = flow.onCallback(callbackContext, callback, "ACTION:ADD")
        promptResult!!.stepKey shouldBe NotesStep.MAIN.key
        promptResult.payload.pendingAction shouldBe NotesPendingAction.Add

        val messageContext = FlowMessageContext(
            user = telegramUser,
            locale = locale,
            state = FlowStateHolder(
                stepKey = NotesStep.MAIN.key,
                payload = promptResult.payload,
                messageBindings = mapOf(
                    "notes_main_message" to 10,
                    promptResult.payload.promptBindings.first() to 11
                )
            )
        )
        val message = mockk<Message>(relaxed = true) { every { text } returns "new note"; every { messageId } returns 22 }

        flow.onMessage(messageContext, message)

        verify { userService.addNote(101L, "new note") }
    }

    should("remove note when user enters index") {
        val stateHolder = FlowStateHolder(
            stepKey = NotesStep.MAIN.key,
            payload = NotesFlowState(),
            messageBindings = mapOf("notes_main_message" to 10)
        )
        val callbackContext = FlowCallbackContext(
            user = telegramUser,
            locale = locale,
            state = stateHolder
        )
        val callback = mockk<CallbackQuery>(relaxed = true) { every { id } returns "cb" }
        val userEntity = User(telegramUser.id, "Tester", null, "tester").apply {
            notes.addAll(listOf("a", "b"))
        }
        every { userService.getUser(101L) } returns userEntity

        val promptResult = flow.onCallback(callbackContext, callback, "ACTION:REMOVE")!!
        val promptBinding = promptResult.actions
            .filterIsInstance<SendMessageAction>()
            .first()
            .bindingKey!!

        val messageContext = FlowMessageContext(
            user = telegramUser,
            locale = locale,
            state = FlowStateHolder(
                stepKey = NotesStep.MAIN.key,
                payload = promptResult.payload,
                messageBindings = mapOf(
                    "notes_main_message" to 10,
                    promptBinding to 12
                )
            )
        )
        val message = mockk<Message>(relaxed = true) { every { text } returns "1"; every { messageId } returns 23 }

        flow.onMessage(messageContext, message)

        verify { userService.removeNote(101L, 1) }
    }

    should("clear notes on callback action") {
        val stateHolder = FlowStateHolder(
            stepKey = NotesStep.MAIN.key,
            payload = NotesFlowState(),
            messageBindings = mapOf("notes_main_message" to 10)
        )
        val callbackContext = FlowCallbackContext(
            user = telegramUser,
            locale = locale,
            state = stateHolder
        )
        val callback = mockk<CallbackQuery>(relaxed = true) { every { id } returns "cb" }

        val result = flow.onCallback(callbackContext, callback, "ACTION:CLEAR")!!

        result.stepKey shouldBe NotesStep.MAIN.key
        verify { userService.clearNotes(telegramUser) }
    }
})


