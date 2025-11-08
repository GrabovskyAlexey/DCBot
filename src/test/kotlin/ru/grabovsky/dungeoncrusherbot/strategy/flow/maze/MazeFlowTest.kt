package ru.grabovsky.dungeoncrusherbot.strategy.flow.maze

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.*
import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.entity.Location
import ru.grabovsky.dungeoncrusherbot.entity.Maze
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MazeService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.*
import java.util.*
import org.telegram.telegrambots.meta.api.objects.User as TgUser
import org.telegram.telegrambots.meta.api.objects.message.Message as TgMessage

class MazeFlowTest : ShouldSpec({
    val mazeService = mockk<MazeService>(relaxed = true)
    val viewService = mockk<MazeViewService>()
    val promptBuilder = mockk<MazePromptBuilder>()
    val i18nService = mockk<I18nService>()

    val flow = MazeFlow(mazeService, viewService, promptBuilder, i18nService)

    val locale = Locale.forLanguageTag("ru")
    val telegramUser = mockk<TgUser>(relaxed = true) { every { id } returns 42L }
    val entityUser = User(42L, "Tester", null, "tester")
    val maze = Maze(user = entityUser)
    entityUser.maze = maze

    val overview = MazeOverviewModel(
        location = Location(level = 0, offset = 0, direction = Direction.CENTER),
        sameStepsEnabled = false,
        steps = emptyList(),
        showHistory = false
    )
    val buttons = listOf(
        MazeButton(label = "\u2196\uFE0F", payload = "MAIN:STEP_LEFT", row = 0, col = 0)
    )
    val mainView = MazeMainView(overview, buttons)

    beforeTest {
        clearMocks(mazeService, viewService, promptBuilder, i18nService)
        every { viewService.buildMainView(telegramUser, locale, any()) } returns mainView
        every { viewService.ensureMaze(telegramUser) } returns maze
        every { i18nService.i18n(any(), any(), any(), *anyVararg()) } answers { thirdArg() ?: "" }
    }

    fun FlowResult<MazeFlowState>.shouldHaveSingleSendMessage(): FlowResult<MazeFlowState> {
        actions shouldHaveSize 1
        val send = actions.first() as ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.SendMessageAction
        send.bindingKey shouldBe "maze_main_message"
        send.message.flowKey shouldBe FlowKeys.MAZE
        send.message.stepKey shouldBe MazeFlowStep.MAIN.key
        send.message.inlineButtons shouldHaveSize 1
        val button = send.message.inlineButtons.first()
        button.payload shouldBe FlowCallbackPayload(FlowKeys.MAZE.value, "MAIN:STEP_LEFT")
        return this
    }

    should("start flow with main view") {
        val result = flow.start(FlowStartContext(telegramUser, locale))

        result.stepKey shouldBe MazeFlowStep.MAIN.key
        result.payload.pendingDirection shouldBe null
        result.shouldHaveSingleSendMessage()
        verify { viewService.ensureMaze(telegramUser) }
        verify { viewService.buildMainView(telegramUser, locale, false) }
    }

    should("process single step callback") {
        val context = FlowContext(
            user = telegramUser,
            locale = locale,
            state = FlowStateHolder(
                stepKey = MazeFlowStep.MAIN.key,
                payload = MazeFlowState(),
                messageBindings = emptyMap()
            )
        )
        val callbackQuery = mockk<org.telegram.telegrambots.meta.api.objects.CallbackQuery>(relaxed = true) {
            every { id } returns "q1"
        }

        justRun { mazeService.processStep(maze, Direction.LEFT) }

        val result = flow.onCallback(context, callbackQuery, "MAIN:STEP_LEFT")

        result shouldNotBe null
        result!!.stepKey shouldBe MazeFlowStep.MAIN.key
        result.payload.pendingDirection shouldBe null
        result.actions.any { it is EditMessageAction } shouldBe true
        result.actions.any { it is AnswerCallbackAction } shouldBe true
        verify { mazeService.processStep(maze, Direction.LEFT) }
        verify { viewService.buildMainView(telegramUser, locale, false) }
    }

    should("enter same step prompt on callback") {
        val state = MazeFlowState()
        val context = FlowContext(
            user = telegramUser,
            locale = locale,
            state = FlowStateHolder(
                stepKey = MazeFlowStep.MAIN.key,
                payload = state,
                messageBindings = emptyMap()
            )
        )
        val callbackQuery = mockk<org.telegram.telegrambots.meta.api.objects.CallbackQuery>(relaxed = true) {
            every { id } returns "q2"
        }
        every { promptBuilder.build(Direction.LEFT, locale, false) } returns MazePromptModel("prompt")
        every { i18nService.i18n("flow.button.cancel", locale, any(), *anyVararg()) } returns "Отмена"

        val result = flow.onCallback(context, callbackQuery, "MAIN:STEP_SAME_LEFT")

        result shouldNotBe null
        result!!.payload.pendingDirection shouldBe Direction.LEFT
        val sendAction = result.actions.filterIsInstance<ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.SendMessageAction>().first()
        sendAction.bindingKey.shouldStartWith("maze_prompt_message")
        sendAction.message.stepKey shouldBe MazeFlowStep.PROMPT.key
        sendAction.message.inlineButtons shouldHaveSize 1
        sendAction.message.inlineButtons.first().payload shouldBe FlowCallbackPayload(FlowKeys.MAZE.value, "PROMPT:CANCEL")
    }

    should("retry prompt when message is invalid") {
        val state = MazeFlowState(pendingDirection = Direction.LEFT)
        val context = FlowContext(
            user = telegramUser,
            locale = locale,
            state = FlowStateHolder(
                stepKey = MazeFlowStep.MAIN.key,
                payload = state,
                messageBindings = emptyMap()
            )
        )
        val message = mockk<TgMessage>(relaxed = true) { every { messageId } returns 101; every { text } returns "oops" }
        every { promptBuilder.build(Direction.LEFT, locale, true) } returns MazePromptModel("invalid")

        val result = flow.onMessage(context, message)

        result shouldNotBe null
        val sendAction = result!!.actions.filterIsInstance<ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.SendMessageAction>().first()
        sendAction.message.model shouldBe MazePromptModel("invalid")
        state.pendingDirection shouldBe Direction.LEFT
    }

    should("process same steps input") {
        val state = MazeFlowState(pendingDirection = Direction.LEFT, promptBindings = mutableListOf("maze_prompt_message_1"))
        val context = FlowContext(
            user = telegramUser,
            locale = locale,
            state = FlowStateHolder(
                stepKey = MazeFlowStep.MAIN.key,
                payload = state,
                messageBindings = mapOf("maze_prompt_message_1" to 55)
            )
        )
        val message = mockk<TgMessage>(relaxed = true) { every { messageId } returns 200; every { text } returns "3" }
        justRun { mazeService.processSameStep(maze, Direction.LEFT, 3) }

        val result = flow.onMessage(context, message)

        result shouldNotBe null
        result!!.payload.pendingDirection shouldBe null
        result.actions.any { it is EditMessageAction } shouldBe true
        result.actions.any { it is AnswerCallbackAction } shouldBe false
        verify { mazeService.processSameStep(maze, Direction.LEFT, 3) }
        verify { viewService.buildMainView(telegramUser, locale, false) }
    }

    should("show confirm reset view") {
        val state = MazeFlowState()
        val context = FlowContext(
            user = telegramUser,
            locale = locale,
            state = FlowStateHolder(
                stepKey = MazeFlowStep.MAIN.key,
                payload = state,
                messageBindings = emptyMap()
            )
        )
        val callbackQuery = mockk<org.telegram.telegrambots.meta.api.objects.CallbackQuery>(relaxed = true) {
            every { id } returns "q3"
        }

        val result = flow.onCallback(context, callbackQuery, "MAIN:RESET")

        result shouldNotBe null
        result!!.stepKey shouldBe MazeFlowStep.CONFIRM_RESET.key
        val edit = result.actions.filterIsInstance<EditMessageAction>().first()
        edit.message.stepKey shouldBe MazeFlowStep.CONFIRM_RESET.key
        result.actions.any { it is AnswerCallbackAction } shouldBe true
    }

    should("confirm reset and refresh maze") {
        val state = MazeFlowState()
        val context = FlowContext(
            user = telegramUser,
            locale = locale,
            state = FlowStateHolder(
                stepKey = MazeFlowStep.CONFIRM_RESET.key,
                payload = state,
                messageBindings = emptyMap()
            )
        )
        val callbackQuery = mockk<org.telegram.telegrambots.meta.api.objects.CallbackQuery>(relaxed = true) {
            every { id } returns "q4"
        }
        justRun { mazeService.refreshMaze(maze) }

        val result = flow.onCallback(context, callbackQuery, "CONFIRM:CONFIRM")

        result shouldNotBe null
        result!!.stepKey shouldBe MazeFlowStep.MAIN.key
        verify { mazeService.refreshMaze(maze) }
        verify { viewService.buildMainView(telegramUser, locale, false) }
    }
})
