package ru.grabovsky.dungeoncrusherbot.strategy.flow.notes

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
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
class NotesFlow(
    private val userService: UserService,
    private val viewService: NotesViewService,
    private val promptBuilder: NotesPromptBuilder,
    private val i18nService: I18nService,
) : FlowHandler<NotesFlowState> {

    override val key: FlowKey = FlowKeys.NOTES
    override val payloadType: Class<NotesFlowState> = NotesFlowState::class.java

    override fun start(context: FlowStartContext): FlowResult<NotesFlowState> {
        val overview = viewService.buildOverview(context.user, context.locale)
        return buildFlowResult(
            NotesStep.MAIN,
            NotesFlowState(),
            listOf(
                SendMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildMainMessage(overview)
                )
            )
        )
    }

    override fun onMessage(context: FlowMessageContext<NotesFlowState>, message: Message): FlowResult<NotesFlowState>? {
        return when (context.state.payload.pendingAction) {
            NotesPendingAction.Add -> handleAddInput(context, message)
            NotesPendingAction.Remove -> handleRemoveInput(context, message)
            null -> null
        }
    }

    override fun onCallback(
        context: FlowCallbackContext<NotesFlowState>,
        callbackQuery: CallbackQuery,
        data: String
    ): FlowResult<NotesFlowState>? {
        val (command, argument) = parseCallback(data)
        return when (command) {
            "ACTION" -> argument?.let { handleAction(context, callbackQuery, it) }
            "PROMPT" -> handlePromptCallback(context.state.payload, callbackQuery, argument)
            else -> null
        }
    }

    private fun handleAction(
        context: FlowCallbackContext<NotesFlowState>,
        callbackQuery: CallbackQuery,
        action: String
    ): FlowResult<NotesFlowState>? {
        return when (action) {
            "ADD" -> enterAddPrompt(context, callbackQuery)
            "REMOVE" -> enterRemovePrompt(context, callbackQuery)
            "CLEAR" -> clearNotes(context, callbackQuery)
            else -> null
        }
    }

    private fun handlePromptCallback(
        state: NotesFlowState,
        callbackQuery: CallbackQuery,
        argument: String?
    ): FlowResult<NotesFlowState>? {
        return when (argument) {
            "CANCEL" -> cancelPrompt(state, callbackQuery)
            else -> null
        }
    }

    private fun handleAddInput(
        context: FlowMessageContext<NotesFlowState>,
        message: Message
    ): FlowResult<NotesFlowState> {
        val value = message.text?.trim().orEmpty()
        if (value.isBlank()) {
            val prompt = promptBuilder.addPrompt(context.locale, invalid = true)
            return retryPrompt(context, prompt, message)
        }

        val added = userService.addNote(context.user.id, value)
        if (!added) {
            return finalizePrompt(context, message.messageId)
        }
        return finalizePrompt(context, message.messageId)
    }

    private fun handleRemoveInput(
        context: FlowMessageContext<NotesFlowState>,
        message: Message
    ): FlowResult<NotesFlowState> {
        val index = message.text?.toIntOrNull()
        val user = userService.getUser(context.user.id)
        val notes = user?.profile?.notes?.toList().orEmpty()
        if (index == null || index <= 0 || index > notes.size) {
            val prompt = promptBuilder.removePrompt(context.locale, notes.toList(), invalid = true)
            return retryPrompt(context, prompt, message)
        }

        userService.removeNote(context.user.id, index)
        return finalizePrompt(context, message.messageId)
    }

    private fun enterAddPrompt(
        context: FlowCallbackContext<NotesFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<NotesFlowState> {
        val prompt = promptBuilder.addPrompt(context.locale)
        return startPrompt(context, NotesPendingAction.Add, prompt, callbackQuery)
    }

    private fun enterRemovePrompt(
        context: FlowCallbackContext<NotesFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<NotesFlowState>? {
        val user = userService.getUser(context.user.id)
        val notes = user?.profile?.notes?.toList().orEmpty()
        if (notes.isEmpty()) {
            return buildFlowResult(
                NotesStep.MAIN,
                context.state.payload,
                listOf(
                    AnswerCallbackAction(
                        callbackQueryId = callbackQuery.id,
                        text = i18nService.i18n("flow.notes.error.no_notes", context.locale, "Заметок пока нет"),
                        showAlert = true
                    )
                )
            )
        }
        val prompt = promptBuilder.removePrompt(context.locale, notes, invalid = false)
        return startPrompt(context, NotesPendingAction.Remove, prompt, callbackQuery)
    }

    private fun clearNotes(
        context: FlowCallbackContext<NotesFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<NotesFlowState> {
        userService.clearNotes(context.user)
        val overview = viewService.buildOverview(context.user, context.locale)
        return buildFlowResult(
            NotesStep.MAIN,
            context.state.payload.apply { pendingAction = null },
            listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildMainMessage(overview)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun retryPrompt(
        context: FlowMessageContext<NotesFlowState>,
        prompt: NotesPromptModel,
        message: Message
    ): FlowResult<NotesFlowState> {
        val state = context.state.payload
        val promptBinding = nextPromptBinding()
        state.promptBindings.add(promptBinding)
        return FlowResult(
            stepKey = NotesStep.MAIN.key,
            payload = state,
            actions = listOf(
                SendMessageAction(
                    bindingKey = promptBinding,
                    message = FlowMessage(
                        flowKey = key,
                        stepKey = NotesStep.PROMPT_TEXT.key,
                        model = prompt,
                        inlineButtons = promptButtons(context.locale)
                    )
                ),
                DeleteMessageIdAction(message.messageId)
            )
        )
    }

    private fun finalizePrompt(
        context: FlowMessageContext<NotesFlowState>,
        userMessageId: Int?
    ): FlowResult<NotesFlowState> {
        val state = context.state.payload
        val cleanup = FlowUtils.cleanupPromptActions(state.promptBindings)
        userMessageId?.let { cleanup += DeleteMessageIdAction(it) }
        state.promptBindings.clear()
        state.pendingAction = null

        val overview = viewService.buildOverview(context.user, context.locale)
        cleanup += EditMessageAction(
            bindingKey = MAIN_MESSAGE_KEY,
            message = buildMainMessage(overview)
        )

        return buildFlowResult(NotesStep.MAIN, state, cleanup)
    }

    private fun cancelPrompt(
        state: NotesFlowState,
        callbackQuery: CallbackQuery
    ): FlowResult<NotesFlowState> {
        val cleanup = FlowUtils.cleanupPromptActions(state.promptBindings)
        state.promptBindings.clear()
        state.pendingAction = null
        cleanup += AnswerCallbackAction(callbackQuery.id)
        return FlowResult(
            stepKey = NotesStep.MAIN.key,
            payload = state,
            actions = cleanup
        )
    }

    private fun startPrompt(
        context: FlowCallbackContext<NotesFlowState>,
        pendingAction: NotesPendingAction,
        prompt: NotesPromptModel,
        callbackQuery: CallbackQuery
    ): FlowResult<NotesFlowState> {
        val state = context.state.payload
        val promptBinding = nextPromptBinding()
        state.promptBindings.add(promptBinding)
        state.pendingAction = pendingAction
        return buildFlowResult(
            NotesStep.MAIN,
            state,
            listOf(
                SendMessageAction(
                    bindingKey = promptBinding,
                    message = FlowMessage(
                        flowKey = key,
                        stepKey = NotesStep.PROMPT_TEXT.key,
                        model = prompt,
                        inlineButtons = promptButtons(context.locale)
                    )
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun buildMainMessage(overview: NotesOverviewModel): FlowMessage =
        FlowMessage(
            flowKey = key,
            stepKey = NotesStep.MAIN.key,
            model = overview,
            inlineButtons = overview.buttons.inlineButtons()
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

    private fun List<NoteButton>.inlineButtons(): List<FlowInlineButton> =
        map { button ->
            FlowInlineButton(
                text = button.label,
                payload = FlowCallbackPayload(key.value, "ACTION:${button.action}"),
                row = button.row,
                col = button.col
            )
        }

    private fun parseCallback(data: String): Pair<String, String?> =
        if (data.contains(':')) {
            val split = data.split(':', limit = 2)
            split[0] to split[1]
        } else {
            data to null
        }

    private fun buildFlowResult(
        step: NotesStep,
        payload: NotesFlowState,
        actions: List<FlowAction>
    ) = FlowResult(
        stepKey = step.key,
        payload = payload,
        actions = actions
    )

    private fun nextPromptBinding(): String = "${PROMPT_MESSAGE_KEY}_${UUID.randomUUID()}"

    companion object {
        private const val MAIN_MESSAGE_KEY = "notes_main_message"
        private const val PROMPT_MESSAGE_KEY = "notes_prompt_message"
    }
}
