package ru.grabovsky.dungeoncrusherbot.strategy.flow.maze

import java.util.Locale
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.entity.Maze
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MazeService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.*
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.buildMessage
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cancelPrompt
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cancelPromptButton
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cleanupPromptMessages
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.finalizePrompt
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.retryPrompt
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.startPrompt

@Component
class MazeFlow(
    private val mazeService: MazeService,
    private val viewService: MazeViewService,
    private val promptBuilder: MazePromptBuilder,
    private val i18nService: I18nService,
) : FlowHandler<MazeFlowState> {

    override val key: FlowKey = FlowKeys.MAZE
    override val payloadType: Class<MazeFlowState> = MazeFlowState::class.java

    override fun start(context: FlowStartContext): FlowResult<MazeFlowState> {
        viewService.ensureMaze(context.user)
        val view = viewService.buildMainView(context.user, context.locale, showHistory = false)
        return buildFlowResult(
            MazeFlowStep.MAIN,
            MazeFlowState(),
            listOf(
                SendMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildMainMessage(view, context.locale)
                )
            )
        )
    }

    override fun onMessage(context: FlowMessageContext<MazeFlowState>, message: Message): FlowResult<MazeFlowState>? {
        val state = context.state.payload
        val direction = state.pendingDirection ?: return null
        val text = message.text?.trim().orEmpty()
        val steps = text.toIntOrNull()
        if (steps == null || steps !in SAME_STEPS_RANGE) {
            val prompt = promptBuilder.build(direction, context.locale, invalid = true)
            return retryPrompt(context, prompt, message)
        }

        val maze = ensureMaze(context) ?: return finalizePrompt(context, message.messageId)
        mazeService.processSameStep(maze, direction, steps)
        return finalizePrompt(context, message.messageId)
    }

    override fun onCallback(
        context: FlowCallbackContext<MazeFlowState>,
        callbackQuery: CallbackQuery,
        data: String
    ): FlowResult<MazeFlowState>? {
        val (command, argument) = parseCallback(data)
        return when (command) {
            "MAIN" -> argument?.let { handleMainAction(context, callbackQuery, it) }
            "PROMPT" -> handlePromptAction(context, callbackQuery, argument)
            "CONFIRM" -> argument?.let { handleConfirmAction(context, callbackQuery, it) }
            else -> null
        }
    }

    private fun handleMainAction(
        context: FlowCallbackContext<MazeFlowState>,
        callbackQuery: CallbackQuery,
        action: String
    ): FlowResult<MazeFlowState>? =
        when (action) {
            "STEP_LEFT" -> processStep(context, callbackQuery, Direction.LEFT)
            "STEP_CENTER" -> processStep(context, callbackQuery, Direction.CENTER)
            "STEP_RIGHT" -> processStep(context, callbackQuery, Direction.RIGHT)
            "STEP_SAME_LEFT" -> enterSameStepPrompt(context, callbackQuery, Direction.LEFT)
            "STEP_SAME_CENTER" -> enterSameStepPrompt(context, callbackQuery, Direction.CENTER)
            "STEP_SAME_RIGHT" -> enterSameStepPrompt(context, callbackQuery, Direction.RIGHT)
            "TOGGLE_SAME" -> toggleSameSteps(context, callbackQuery)
            "HISTORY" -> showMain(context, callbackQuery, showHistory = true)
            "RESET" -> showConfirmReset(context, callbackQuery)
            else -> null
        }

    private fun handlePromptAction(
        context: FlowCallbackContext<MazeFlowState>,
        callbackQuery: CallbackQuery,
        argument: String?
    ): FlowResult<MazeFlowState>? =
        when (argument) {
            "CANCEL" -> cancelPrompt(context, callbackQuery)
            else -> null
        }

    private fun handleConfirmAction(
        context: FlowCallbackContext<MazeFlowState>,
        callbackQuery: CallbackQuery,
        action: String
    ): FlowResult<MazeFlowState>? =
        when (action) {
            "CONFIRM" -> confirmReset(context, callbackQuery)
            "CANCEL" -> showMain(context, callbackQuery, showHistory = false)
            else -> null
        }

    private fun processStep(
        context: FlowCallbackContext<MazeFlowState>,
        callbackQuery: CallbackQuery,
        direction: Direction
    ): FlowResult<MazeFlowState>? {
        val maze = ensureMaze(context) ?: return null
        mazeService.processStep(maze, direction)
        val state = context.state.payload
        val actions = state.cleanupPromptMessages()
        state.pendingDirection = null
        return buildMainResult(context, state, showHistory = false, callbackQuery = callbackQuery, actions = actions)
    }

    private fun enterSameStepPrompt(
        context: FlowCallbackContext<MazeFlowState>,
        callbackQuery: CallbackQuery,
        direction: Direction
    ): FlowResult<MazeFlowState> {
        val state = context.state.payload
        val cleanup = state.cleanupPromptMessages()
        return context.startPrompt(
            targetStep = MazeFlowStep.MAIN,
            bindingPrefix = PROMPT_MESSAGE_KEY,
            callbackQuery = callbackQuery,
            updateState = {
                pendingDirection = direction
            },
            appendActions = { addAll(cleanup) }
        ) {
            key.buildMessage(
                step = MazeFlowStep.PROMPT,
                model = promptBuilder.build(direction, context.locale),
                inlineButtons = promptButtons(context.locale)
            )
        }
    }

    private fun toggleSameSteps(
        context: FlowCallbackContext<MazeFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<MazeFlowState>? {
        val maze = ensureMaze(context) ?: return null
        val state = context.state.payload
        val actions = state.cleanupPromptMessages()
        state.pendingDirection = null
        mazeService.revertSameSteps(maze)
        return buildMainResult(context, state, showHistory = false, callbackQuery = callbackQuery, actions = actions)
    }

    private fun showMain(
        context: FlowCallbackContext<MazeFlowState>,
        callbackQuery: CallbackQuery,
        showHistory: Boolean
    ): FlowResult<MazeFlowState> {
        val state = context.state.payload
        val actions = state.cleanupPromptMessages()
        state.pendingDirection = null
        return buildMainResult(context, state, showHistory = showHistory, callbackQuery = callbackQuery, actions = actions)
    }

    private fun showConfirmReset(
        context: FlowCallbackContext<MazeFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<MazeFlowState> {
        val state = context.state.payload
        val actions = state.cleanupPromptMessages()
        state.pendingDirection = null
        actions += EditMessageAction(
            bindingKey = MAIN_MESSAGE_KEY,
            message = key.buildMessage(
                step = MazeFlowStep.CONFIRM_RESET,
                model = null,
                inlineButtons = confirmButtons(context.locale)
            )
        )
        actions += AnswerCallbackAction(callbackQuery.id)
        return buildFlowResult(MazeFlowStep.CONFIRM_RESET, state, actions)
    }

    private fun confirmReset(
        context: FlowCallbackContext<MazeFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<MazeFlowState>? {
        val maze = ensureMaze(context) ?: return null
        mazeService.refreshMaze(maze)
        val state = context.state.payload
        val actions = state.cleanupPromptMessages()
        state.pendingDirection = null
        return buildMainResult(context, state, showHistory = false, callbackQuery = callbackQuery, actions = actions)
    }

    private fun retryPrompt(
        context: FlowMessageContext<MazeFlowState>,
        prompt: MazePromptModel,
        message: Message
    ): FlowResult<MazeFlowState> {
        val cleanup = context.state.payload.cleanupPromptMessages()
        return context.retryPrompt(
            targetStep = MazeFlowStep.MAIN,
            bindingPrefix = PROMPT_MESSAGE_KEY,
            userMessageId = message.messageId,
            appendActions = { addAll(cleanup) }
        ) {
            key.buildMessage(
                step = MazeFlowStep.PROMPT,
                model = prompt,
                inlineButtons = promptButtons(context.locale)
            )
        }
    }

    private fun finalizePrompt(
        context: FlowMessageContext<MazeFlowState>,
        userMessageId: Int?
    ): FlowResult<MazeFlowState> =
        context.finalizePrompt(
            targetStep = MazeFlowStep.MAIN,
            userMessageId = userMessageId,
            updateState = { pendingDirection = null }
        ) {
            val view = viewService.buildMainView(context.user, context.locale, showHistory = false)
            this += mainViewEditAction(view, context.locale)
        }

    private fun cancelPrompt(
        context: FlowCallbackContext<MazeFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<MazeFlowState> =
        context.cancelPrompt(
            targetStep = MazeFlowStep.MAIN,
            callbackQuery = callbackQuery,
            updateState = { pendingDirection = null }
        )

    private fun buildMainMessage(view: MazeMainView, locale: Locale): FlowMessage =
        key.buildMessage(
            step = MazeFlowStep.MAIN,
            model = view.overview,
            inlineButtons = view.buttons.inlineButtons()
        )

    private fun mainViewEditAction(view: MazeMainView, locale: Locale): EditMessageAction =
        EditMessageAction(
            bindingKey = MAIN_MESSAGE_KEY,
            message = buildMainMessage(view, locale)
        )

    private fun buildMainResult(
        context: FlowCallbackContext<MazeFlowState>,
        state: MazeFlowState,
        showHistory: Boolean,
        callbackQuery: CallbackQuery,
        actions: MutableList<FlowAction>
    ): FlowResult<MazeFlowState> {
        val view = viewService.buildMainView(context.user, context.locale, showHistory)
        actions += mainViewEditAction(view, context.locale)
        actions += AnswerCallbackAction(callbackQuery.id)
        return buildFlowResult(MazeFlowStep.MAIN, state, actions)
    }

    private fun promptButtons(locale: Locale): List<FlowInlineButton> =
        listOf(
            key.cancelPromptButton(
                text = i18nService.i18n("flow.button.cancel", locale, "❌Отмена")
            )
        )

    private fun confirmButtons(locale: Locale): List<FlowInlineButton> =
        listOf(
            FlowInlineButton(
                text = i18nService.i18n("buttons.maze.confirm_refresh.confirm", locale, "✅ДА"),
                payload = FlowCallbackPayload(key.value, "CONFIRM:CONFIRM"),
                row = 0,
                col = 0
            ),
            FlowInlineButton(
                text = i18nService.i18n("buttons.maze.confirm_refresh.cancel", locale, "❌НЕТ"),
                payload = FlowCallbackPayload(key.value, "CONFIRM:CANCEL"),
                row = 0,
                col = 1
            )
        )

    private fun List<MazeButton>.inlineButtons(): List<FlowInlineButton> =
        map { button ->
            FlowInlineButton(
                text = button.label,
                payload = FlowCallbackPayload(key.value, button.payload),
                row = button.row,
                col = button.col
            )
        }

    private fun ensureMaze(context: FlowCallbackContext<MazeFlowState>): Maze? =
        viewService.ensureMaze(context.user)

    private fun ensureMaze(context: FlowMessageContext<MazeFlowState>): Maze? =
        viewService.ensureMaze(context.user)

    private fun parseCallback(data: String): Pair<String, String?> =
        if (data.contains(':')) {
            val split = data.split(':', limit = 2)
            split[0] to split[1]
        } else {
            data to null
        }

    private fun buildFlowResult(
        step: MazeFlowStep,
        payload: MazeFlowState,
        actions: List<FlowAction>
    ) = FlowResult(
        stepKey = step.key,
        payload = payload,
        actions = actions
    )

    companion object {
        private const val MAIN_MESSAGE_KEY = "maze_main_message"
        private const val PROMPT_MESSAGE_KEY = "maze_prompt_message"
        private val SAME_STEPS_RANGE = 1..10
    }
}
