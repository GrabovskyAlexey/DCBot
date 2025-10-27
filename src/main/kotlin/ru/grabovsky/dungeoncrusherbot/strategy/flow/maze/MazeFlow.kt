package ru.grabovsky.dungeoncrusherbot.strategy.flow.maze

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.entity.Maze
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MazeService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.AnswerCallbackAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.DeleteMessageIdAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.EditMessageAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowCallbackContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowCallbackPayload
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowHandler
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowInlineButton
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKey
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowMessage
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowMessageContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowResult
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStartContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.SendMessageAction
import ru.grabovsky.dungeoncrusherbot.util.FlowUtils
import java.util.Locale
import java.util.UUID

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
            "PROMPT" -> handlePromptAction(context.state.payload, callbackQuery, argument)
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
        state: MazeFlowState,
        callbackQuery: CallbackQuery,
        argument: String?
    ): FlowResult<MazeFlowState>? =
        when (argument) {
            "CANCEL" -> cancelPrompt(state, callbackQuery)
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
        val actions = resetPromptState(context.state.payload)
        val view = viewService.buildMainView(context.user, context.locale, showHistory = false)
        actions += EditMessageAction(
            bindingKey = MAIN_MESSAGE_KEY,
            message = buildMainMessage(view, context.locale)
        )
        actions += AnswerCallbackAction(callbackQuery.id)
        return buildFlowResult(MazeFlowStep.MAIN, context.state.payload, actions)
    }

    private fun enterSameStepPrompt(
        context: FlowCallbackContext<MazeFlowState>,
        callbackQuery: CallbackQuery,
        direction: Direction
    ): FlowResult<MazeFlowState> {
        val state = context.state.payload
        val actions = resetPromptState(state)
        val promptBinding = nextPromptBinding()
        state.promptBindings.add(promptBinding)
        state.pendingDirection = direction
        val prompt = promptBuilder.build(direction, context.locale)
        actions += SendMessageAction(
            bindingKey = promptBinding,
            message = FlowMessage(
                flowKey = key,
                stepKey = MazeFlowStep.PROMPT.key,
                model = prompt,
                inlineButtons = promptButtons(context.locale)
            )
        )
        actions += AnswerCallbackAction(callbackQuery.id)
        return buildFlowResult(MazeFlowStep.MAIN, state, actions)
    }

    private fun toggleSameSteps(
        context: FlowCallbackContext<MazeFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<MazeFlowState>? {
        val maze = ensureMaze(context) ?: return null
        val actions = resetPromptState(context.state.payload)
        mazeService.revertSameSteps(maze)
        val view = viewService.buildMainView(context.user, context.locale, showHistory = false)
        actions += EditMessageAction(
            bindingKey = MAIN_MESSAGE_KEY,
            message = buildMainMessage(view, context.locale)
        )
        actions += AnswerCallbackAction(callbackQuery.id)
        return buildFlowResult(MazeFlowStep.MAIN, context.state.payload, actions)
    }

    private fun showMain(
        context: FlowCallbackContext<MazeFlowState>,
        callbackQuery: CallbackQuery,
        showHistory: Boolean
    ): FlowResult<MazeFlowState> {
        val actions = resetPromptState(context.state.payload)
        val view = viewService.buildMainView(context.user, context.locale, showHistory)
        actions += EditMessageAction(
            bindingKey = MAIN_MESSAGE_KEY,
            message = buildMainMessage(view, context.locale)
        )
        actions += AnswerCallbackAction(callbackQuery.id)
        return buildFlowResult(MazeFlowStep.MAIN, context.state.payload, actions)
    }

    private fun showConfirmReset(
        context: FlowCallbackContext<MazeFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<MazeFlowState> {
        val actions = resetPromptState(context.state.payload)
        actions += EditMessageAction(
            bindingKey = MAIN_MESSAGE_KEY,
            message = FlowMessage(
                flowKey = key,
                stepKey = MazeFlowStep.CONFIRM_RESET.key,
                model = null,
                inlineButtons = confirmButtons(context.locale)
            )
        )
        actions += AnswerCallbackAction(callbackQuery.id)
        return buildFlowResult(MazeFlowStep.CONFIRM_RESET, context.state.payload, actions)
    }

    private fun confirmReset(
        context: FlowCallbackContext<MazeFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<MazeFlowState>? {
        val maze = ensureMaze(context) ?: return null
        mazeService.refreshMaze(maze)
        val actions = resetPromptState(context.state.payload)
        val view = viewService.buildMainView(context.user, context.locale, showHistory = false)
        actions += EditMessageAction(
            bindingKey = MAIN_MESSAGE_KEY,
            message = buildMainMessage(view, context.locale)
        )
        actions += AnswerCallbackAction(callbackQuery.id)
        return buildFlowResult(MazeFlowStep.MAIN, context.state.payload, actions)
    }

    private fun retryPrompt(
        context: FlowMessageContext<MazeFlowState>,
        prompt: MazePromptModel,
        message: Message
    ): FlowResult<MazeFlowState> {
        val state = context.state.payload
        val promptBinding = nextPromptBinding()
        state.promptBindings.add(promptBinding)
        return FlowResult(
            stepKey = MazeFlowStep.MAIN.key,
            payload = state,
            actions = listOf(
                SendMessageAction(
                    bindingKey = promptBinding,
                    message = FlowMessage(
                        flowKey = key,
                        stepKey = MazeFlowStep.PROMPT.key,
                        model = prompt,
                        inlineButtons = promptButtons(context.locale)
                    )
                ),
                DeleteMessageIdAction(message.messageId)
            )
        )
    }

    private fun finalizePrompt(
        context: FlowMessageContext<MazeFlowState>,
        userMessageId: Int?
    ): FlowResult<MazeFlowState> {
        val state = context.state.payload
        val cleanup = resetPromptState(state)
        userMessageId?.let { cleanup += DeleteMessageIdAction(it) }
        val view = viewService.buildMainView(context.user, context.locale, showHistory = false)
        cleanup += EditMessageAction(
            bindingKey = MAIN_MESSAGE_KEY,
            message = buildMainMessage(view, context.locale)
        )
        return buildFlowResult(MazeFlowStep.MAIN, state, cleanup)
    }

    private fun cancelPrompt(
        state: MazeFlowState,
        callbackQuery: CallbackQuery
    ): FlowResult<MazeFlowState> {
        val cleanup = resetPromptState(state)
        cleanup += AnswerCallbackAction(callbackQuery.id)
        return FlowResult(
            stepKey = MazeFlowStep.MAIN.key,
            payload = state,
            actions = cleanup
        )
    }

    private fun buildMainMessage(view: MazeMainView, locale: Locale): FlowMessage =
        FlowMessage(
            flowKey = key,
            stepKey = MazeFlowStep.MAIN.key,
            model = view.overview,
            inlineButtons = view.buttons.inlineButtons()
        )

    private fun promptButtons(locale: Locale): List<FlowInlineButton> =
        listOf(
            FlowInlineButton(
                text = i18nService.i18n("flow.button.cancel", locale, "Отмена"),
                payload = FlowCallbackPayload(key.value, "PROMPT:CANCEL"),
                row = 0,
                col = 0
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

    private fun resetPromptState(state: MazeFlowState): MutableList<FlowAction> {
        val cleanup = FlowUtils.cleanupPromptActions(state.promptBindings)
        state.promptBindings.clear()
        state.pendingDirection = null
        return cleanup
    }

    private fun nextPromptBinding(): String = "${PROMPT_MESSAGE_KEY}_${UUID.randomUUID()}"

    companion object {
        private const val MAIN_MESSAGE_KEY = "maze_main_message"
        private const val PROMPT_MESSAGE_KEY = "maze_prompt_message"
        private val SAME_STEPS_RANGE = 1..10
    }
}
