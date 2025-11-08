package ru.grabovsky.dungeoncrusherbot.strategy.flow.notes

import java.util.Locale
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.*
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.buildMessage
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cancelPrompt
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cancelPromptButton
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cleanupPromptMessages
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.finalizePrompt
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.retryPrompt
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.startPrompt

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
            NotesFlowState(),
            listOf(
                SendMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildMainMessage(overview)
                )
            )
        )
    }

    override fun onMessage(context: FlowContext<NotesFlowState>, message: Message): FlowResult<NotesFlowState>? {
        return when (context.state.payload.pendingAction) {
            NotesPendingAction.Add -> handleAddInput(context, message)
            NotesPendingAction.Remove -> handleRemoveInput(context, message)
            null -> null
        }
    }

    override fun onCallback(
        context: FlowContext<NotesFlowState>,
        callbackQuery: CallbackQuery,
        data: String
    ): FlowResult<NotesFlowState>? {
        val (command, argument) = parseCallback(data)
        return when (command) {
            "ACTION" -> argument?.let { handleAction(context, callbackQuery, it) }
            "PROMPT" -> handlePromptCallback(context, callbackQuery, argument)
            else -> null
        }
    }

    private fun handleAction(
        context: FlowContext<NotesFlowState>,
        callbackQuery: CallbackQuery,
        action: String
    ): FlowResult<NotesFlowState>? =
        when (action) {
            "ADD" -> enterAddPrompt(context, callbackQuery)
            "REMOVE" -> enterRemovePrompt(context, callbackQuery)
            "CLEAR" -> clearNotes(context, callbackQuery)
            else -> null
        }

    private fun handlePromptCallback(
        context: FlowContext<NotesFlowState>,
        callbackQuery: CallbackQuery,
        argument: String?
    ): FlowResult<NotesFlowState>? =
        when (argument) {
            "CANCEL" -> cancelPrompt(context, callbackQuery)
            else -> null
        }

    private fun handleAddInput(
        context: FlowContext<NotesFlowState>,
        message: Message
    ): FlowResult<NotesFlowState> {
        val value = message.text?.trim().orEmpty()
        if (value.isBlank()) {
            val prompt = promptBuilder.addPrompt(context.locale, invalid = true)
            return context.retryNotesPrompt(prompt, message)
        }

        userService.addNote(context.user.id, value)

        return context.finalizeNotesPrompt(message.messageId)
    }

    private fun handleRemoveInput(
        context: FlowContext<NotesFlowState>,
        message: Message
    ): FlowResult<NotesFlowState> {
        val index = message.text?.toIntOrNull()
        val user = userService.getUser(context.user.id)
        val notes = user?.profile?.notes?.toList().orEmpty()
        if (index == null || index <= 0 || index > notes.size) {
            val prompt = promptBuilder.removePrompt(context.locale, notes, invalid = true)
            return context.retryNotesPrompt(prompt, message)
        }

        userService.removeNote(context.user.id, index)

        return context.finalizeNotesPrompt(message.messageId)
    }

    private fun enterAddPrompt(
        context: FlowContext<NotesFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<NotesFlowState> {
        val prompt = promptBuilder.addPrompt(context.locale)
        return context.startNotesPrompt(
            callbackQuery = callbackQuery,
            pending = NotesPendingAction.Add,
            prompt = prompt
        )
    }

    private fun enterRemovePrompt(
        context: FlowContext<NotesFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<NotesFlowState> {
        val user = userService.getUser(context.user.id)
        val notes = user?.profile?.notes?.toList().orEmpty()
        if (notes.isEmpty()) {
            return buildFlowResult(
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
        return context.startNotesPrompt(
            callbackQuery = callbackQuery,
            pending = NotesPendingAction.Remove,
            prompt = prompt
        )
    }

    private fun clearNotes(
        context: FlowContext<NotesFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<NotesFlowState> {
        userService.clearNotes(context.user)
        val overview = viewService.buildOverview(context.user, context.locale)
        return buildFlowResult(
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

    private fun cancelPrompt(
        context: FlowContext<NotesFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<NotesFlowState> =
        context.cancelPrompt(
            targetStep = NotesStep.MAIN,
            callbackQuery = callbackQuery,
            updateState = { pendingAction = null }
        )

    private fun buildMainMessage(overview: NotesOverviewModel): FlowMessage =
        key.buildMessage(
            step = NotesStep.MAIN,
            model = overview,
            inlineButtons = overview.buttons.inlineButtons()
        )

    private fun buildPromptMessage(prompt: NotesPromptModel, locale: Locale): FlowMessage =
        key.buildMessage(
            step = NotesStep.PROMPT_TEXT,
            model = prompt,
            inlineButtons = promptButtons(locale)
        )

    private fun promptButtons(locale: Locale): List<FlowInlineButton> =
        listOf(
            key.cancelPromptButton(
                text = i18nService.i18n("flow.button.cancel", locale, "❌Отмена")
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

    private fun FlowContext<NotesFlowState>.retryNotesPrompt(
        prompt: NotesPromptModel,
        message: Message
    ): FlowResult<NotesFlowState> =
        retryPrompt(
            targetStep = NotesStep.MAIN,
            bindingPrefix = PROMPT_MESSAGE_KEY,
            userMessageId = message.messageId,
            appendActions = { addAll(state.payload.cleanupPromptMessages()) }
        ) {
            buildPromptMessage(prompt, locale)
        }

    private fun FlowContext<NotesFlowState>.finalizeNotesPrompt(
        userMessageId: Int?
    ): FlowResult<NotesFlowState> =
        finalizePrompt(
            targetStep = NotesStep.MAIN,
            userMessageId = userMessageId,
            updateState = { pendingAction = null }
        ) {
            val overview = viewService.buildOverview(user, locale)
            this += EditMessageAction(
                bindingKey = MAIN_MESSAGE_KEY,
                message = buildMainMessage(overview)
            )
        }

    private fun FlowContext<NotesFlowState>.startNotesPrompt(
        callbackQuery: CallbackQuery,
        pending: NotesPendingAction,
        prompt: NotesPromptModel
    ): FlowResult<NotesFlowState> {
        val cleanup = state.payload.cleanupPromptMessages()
        return startPrompt(
            targetStep = NotesStep.MAIN,
            bindingPrefix = PROMPT_MESSAGE_KEY,
            callbackQuery = callbackQuery,
            updateState = { pendingAction = pending },
            appendActions = { addAll(cleanup) }
        ) {
            buildPromptMessage(prompt, locale)
        }
    }

    private fun buildFlowResult(
        payload: NotesFlowState,
        actions: List<FlowAction>
    ) = FlowResult(
        stepKey = NotesStep.MAIN.key,
        payload = payload,
        actions = actions
    )

    companion object {
        private const val MAIN_MESSAGE_KEY = "notes_main_message"
        private const val PROMPT_MESSAGE_KEY = "notes_prompt_message"
    }
}
