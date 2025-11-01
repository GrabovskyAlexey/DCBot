package ru.grabovsky.dungeoncrusherbot.strategy.flow.resources

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.service.interfaces.AdjustType
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ResourceOperation.*
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ResourcesService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerResourceDto
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.*
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.buildMessage
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cancelPrompt
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cancelPromptButton
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cleanupPromptMessages
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.finalizePrompt
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.retryPrompt
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.startPrompt
import ru.grabovsky.dungeoncrusherbot.strategy.flow.resources.ResourcesPendingAction.*
import java.util.Locale

@Component
class ResourcesFlow(
    private val userService: UserService,
    private val resourcesService: ResourcesService,
    private val viewService: ResourcesViewService,
    private val promptBuilder: ResourcesPromptBuilder,
    private val i18nService: I18nService,
) : FlowHandler<ResourcesFlowState> {

    override val key: FlowKey = FlowKeys.RESOURCES
    override val payloadType: Class<ResourcesFlowState> = ResourcesFlowState::class.java

    override fun start(context: FlowStartContext): FlowResult<ResourcesFlowState> {
        viewService.ensureResources(context.user)
        val overview = viewService.buildOverview(context.user, context.locale)
        return buildFlowResult(ResourcesStep.MAIN, ResourcesFlowState(), listOf(
            SendMessageAction(
                bindingKey = MAIN_MESSAGE_KEY,
                message = buildOverviewMessage(overview)
            )
        ))
    }

    override fun onMessage(context: FlowMessageContext<ResourcesFlowState>, message: Message): FlowResult<ResourcesFlowState>? {
        val pending = context.state.payload.resourcesPendingAction ?: return null
        return when (pending) {
            is Amount -> handleAmountInput(context, message, pending)
            is Exchange -> handleExchangeInput(context, message, pending)
            is AddNote -> handleAddNoteInput(context, message, pending)
            is RemoveNote -> handleRemoveNoteInput(context, message, pending)
        }
    }

    override fun onCallback(
        context: FlowCallbackContext<ResourcesFlowState>,
        callbackQuery: CallbackQuery,
        data: String
    ): FlowResult<ResourcesFlowState>? {
        val payload = context.state.payload
        val (command, argument) = parseCallback(data)
        return when (command) {
            "MAIN" -> showMain(context, callbackQuery)
            "SERVER" -> argument?.toIntOrNull()?.let { showServer(context, it, callbackQuery) }
            "ACTION" -> argument?.let { handleAction(context, payload, it, callbackQuery) }
            "PROMPT" -> handlePromptCallback(context, argument, callbackQuery)
            else -> null
        }
    }

    private fun showMain(
        context: FlowCallbackContext<ResourcesFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<ResourcesFlowState> {
        val overview = viewService.buildOverview(context.user, context.locale)
        return buildFlowResult(ResourcesStep.MAIN, ResourcesFlowState(), listOf(
            EditMessageAction(
                bindingKey = MAIN_MESSAGE_KEY,
                message = buildOverviewMessage(overview)
            ),
            AnswerCallbackAction(callbackQuery.id)
        ))
    }

    private fun showServer(
        context: FlowCallbackContext<ResourcesFlowState>,
        serverId: Int,
        callbackQuery: CallbackQuery
    ): FlowResult<ResourcesFlowState> {
        val detail = viewService.buildServer(context.user, serverId, includeHistory = false, locale = context.locale)
        val state = ResourcesFlowState(selectedServerId = serverId)
        return buildFlowResult(ResourcesStep.SERVER, state, listOf(
            EditMessageAction(
                bindingKey = MAIN_MESSAGE_KEY,
                message = buildServerMessage(detail)
            ),
            AnswerCallbackAction(callbackQuery.id)
        ))
    }

    private fun handleAction(
        context: FlowCallbackContext<ResourcesFlowState>,
        state: ResourcesFlowState,
        actionName: String,
        callbackQuery: CallbackQuery
    ): FlowResult<ResourcesFlowState>? {
        val serverId = state.selectedServerId ?: return null
        state.showHistory = false
        AMOUNT_PROMPT_ACTIONS[actionName]?.let { type ->
            return enterAmountPrompt(context, serverId, type, callbackQuery)
        }

        QUICK_ADJUST_ACTIONS[actionName]?.let { adjustType ->
            return quickAmountOperation(context, state, serverId, adjustType, callbackQuery)
        }

        return when (actionName) {
            "BACK" -> showMain(context, callbackQuery)
            "TOGGLE_NOTIFY" -> applyOperation(context, state, callbackQuery) {
                resourcesService.applyOperation(context.user, serverId, ToggleNotify)
            }
            "SET_MAIN" -> applyOperation(context, state, callbackQuery) {
                resourcesService.applyOperation(context.user, serverId, MarkMain)
            }
            "REMOVE_MAIN" -> applyOperation(context, state, callbackQuery) {
                resourcesService.applyOperation(context.user, serverId, UnmarkMain)
            }
            "REMOVE_EXCHANGE" -> applyOperation(context, state, callbackQuery) {
                resourcesService.applyOperation(context.user, serverId, ClearExchange)
            }
            "SHOW_HISTORY" -> showHistory(context, state, callbackQuery)
            "PROMPT_ADD_EXCHANGE" -> enterExchangePrompt(context, serverId, callbackQuery)
            "PROMPT_ADD_NOTE" -> enterAddNotePrompt(context, serverId, callbackQuery)
            "PROMPT_REMOVE_NOTE" -> enterRemoveNotePrompt(context, serverId, callbackQuery)
            else -> null
        }
    }

    private fun enterAmountPrompt(
        context: FlowCallbackContext<ResourcesFlowState>,
        serverId: Int,
        type: AmountActionType,
        callbackQuery: CallbackQuery
    ): FlowResult<ResourcesFlowState> {
        val prompt = promptBuilder.amountPrompt(context.locale, type, invalid = false)
        return context.startServerPrompt(
            serverId = serverId,
            promptStep = ResourcesStep.PROMPT_AMOUNT,
            callbackQuery = callbackQuery,
            prompt = prompt
        ) {
            resourcesPendingAction = Amount(type, serverId)
        }
    }

    private fun enterExchangePrompt(
        context: FlowCallbackContext<ResourcesFlowState>,
        serverId: Int,
        callbackQuery: CallbackQuery
    ): FlowResult<ResourcesFlowState> {
        val prompt = promptBuilder.exchangePrompt(context.locale, invalid = false)
        return context.startServerPrompt(
            serverId = serverId,
            callbackQuery = callbackQuery,
            prompt = prompt
        ) {
            resourcesPendingAction = Exchange(serverId)
        }
    }

    private fun enterAddNotePrompt(
        context: FlowCallbackContext<ResourcesFlowState>,
        serverId: Int,
        callbackQuery: CallbackQuery
    ): FlowResult<ResourcesFlowState> {
        val prompt = promptBuilder.addNotePrompt(context.locale, invalid = false)
        return context.startServerPrompt(
            serverId = serverId,
            callbackQuery = callbackQuery,
            prompt = prompt
        ) {
            resourcesPendingAction = AddNote(serverId)
        }
    }

    private fun enterRemoveNotePrompt(
        context: FlowCallbackContext<ResourcesFlowState>,
        serverId: Int,
        callbackQuery: CallbackQuery
    ): FlowResult<ResourcesFlowState> {
        val state = context.state.payload
        val user = userService.getUser(context.user.id)
        val notes = user?.profile?.notes?.toList().orEmpty()
        if (notes.isEmpty()) {
            return buildFlowResult(
                ResourcesStep.SERVER,
                state,
                listOf(
                    AnswerCallbackAction(
                        callbackQueryId = callbackQuery.id,
                        text = i18nService.i18n(
                            "flow.notes.error.no_notes",
                            context.locale,
                            "Заметок пока нет"
                        ),
                        showAlert = true
                    )
                )
            )
        }

        val prompt = promptBuilder.removeNotePrompt(context.locale, notes, invalid = false)
        return context.startServerPrompt(
            serverId = serverId,
            callbackQuery = callbackQuery,
            prompt = prompt
        ) {
            resourcesPendingAction = RemoveNote(serverId)
        }
    }

    private fun FlowCallbackContext<ResourcesFlowState>.startServerPrompt(
        serverId: Int,
        callbackQuery: CallbackQuery,
        prompt: ResourcesPromptModel,
        promptStep: ResourcesStep = ResourcesStep.PROMPT_TEXT,
        stateUpdater: ResourcesFlowState.() -> Unit,
    ): FlowResult<ResourcesFlowState> {
        val state = this.state.payload
        val cleanup = state.cleanupPromptMessages()
        return startPrompt(
            targetStep = ResourcesStep.SERVER,
            bindingPrefix = PROMPT_MESSAGE_KEY,
            callbackQuery = callbackQuery,
            updateState = {
                selectedServerId = serverId
                stateUpdater()
            },
            appendActions = { addAll(cleanup) }
        ) {
            buildPromptMessage(promptStep, prompt, locale)
        }
    }

    private fun handlePromptCallback(
        context: FlowCallbackContext<ResourcesFlowState>,
        argument: String?,
        callbackQuery: CallbackQuery
    ): FlowResult<ResourcesFlowState>? =
        when (argument) {
            "CANCEL" -> cancelPrompt(context, callbackQuery)
            else -> null
        }

    private fun handleAmountInput(
        context: FlowMessageContext<ResourcesFlowState>,
        message: Message,
        pending: Amount
    ): FlowResult<ResourcesFlowState> {
        val amount = message.text?.toIntOrNull()
        if (amount == null || amount <= 0) {
            val prompt = promptBuilder.amountPrompt(context.locale, pending.operation, invalid = true)
            return context.retryServerPrompt(ResourcesStep.PROMPT_AMOUNT, prompt, message.messageId)
        }

        val operation = Adjust(pending.operation.toAdjustType(), amount)
        resourcesService.applyOperation(context.user, pending.serverId, operation)

        return rebuildServerAfterPrompt(context, pending.serverId, message.messageId)
    }

    private fun handleExchangeInput(
        context: FlowMessageContext<ResourcesFlowState>,
        message: Message,
        pending: Exchange
    ): FlowResult<ResourcesFlowState> {
        val value = message.text?.trim().orEmpty()
        if (value.isEmpty()) {
            val prompt = promptBuilder.exchangePrompt(context.locale, invalid = true)
            return context.retryServerPrompt(ResourcesStep.PROMPT_TEXT, prompt, message.messageId)
        }

        resourcesService.applyOperation(context.user, pending.serverId, SetExchange(value))
        return rebuildServerAfterPrompt(context, pending.serverId, message.messageId)
    }

    private fun handleAddNoteInput(
        context: FlowMessageContext<ResourcesFlowState>,
        message: Message,
        pending: AddNote
    ): FlowResult<ResourcesFlowState> {
        val value = message.text?.trim().orEmpty()
        if (value.isEmpty()) {
            val prompt = promptBuilder.addNotePrompt(context.locale, invalid = true)
            return context.retryServerPrompt(ResourcesStep.PROMPT_TEXT, prompt, message.messageId)
        }

        userService.addNote(context.user.id, value)
        return rebuildServerAfterPrompt(context, pending.serverId, message.messageId)
    }

    private fun handleRemoveNoteInput(
        context: FlowMessageContext<ResourcesFlowState>,
        message: Message,
        pending: RemoveNote
    ): FlowResult<ResourcesFlowState> {
        val index = message.text?.toIntOrNull()
        val user = userService.getUser(context.user.id)
        val notes = user?.profile?.notes?.toList().orEmpty()
        if (index == null || index <= 0 || index > notes.size) {
            val prompt = promptBuilder.removeNotePrompt(context.locale, notes, invalid = true)
            return context.retryServerPrompt(ResourcesStep.PROMPT_TEXT, prompt, message.messageId)
        }

        userService.removeNote(context.user.id, index)
        return rebuildServerAfterPrompt(context, pending.serverId, message.messageId)
    }
    private fun cancelPrompt(
        context: FlowCallbackContext<ResourcesFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<ResourcesFlowState> {
        val targetStep = if (context.state.payload.selectedServerId != null) {
            ResourcesStep.SERVER
        } else {
            ResourcesStep.MAIN
        }
        return context.cancelPrompt(
            targetStep = targetStep,
            callbackQuery = callbackQuery,
            updateState = { resourcesPendingAction = null }
        )
    }

    private fun rebuildServerAfterPrompt(
        context: FlowMessageContext<ResourcesFlowState>,
        serverId: Int,
        userMessageId: Int?
    ): FlowResult<ResourcesFlowState> =
        context.finalizePrompt(
            targetStep = ResourcesStep.SERVER,
            userMessageId = userMessageId,
            updateState = { resourcesPendingAction = null }
        ) {
            val detail = viewService.buildServer(
                context.user,
                serverId,
                includeHistory = context.state.payload.showHistory,
                locale = context.locale
            )
            this += EditMessageAction(
                bindingKey = MAIN_MESSAGE_KEY,
                message = buildServerMessage(detail)
            )
        }

    private fun buildPromptMessage(
        step: ResourcesStep,
        prompt: ResourcesPromptModel,
        locale: Locale
    ): FlowMessage = key.buildMessage(
        step = step,
        model = prompt,
        inlineButtons = promptButtons(locale)
    )

    private fun buildServerMessage(detail: ServerDetail): FlowMessage = key.buildMessage(
        step = ResourcesStep.SERVER,
        model = ResourcesServerModel(detail),
        inlineButtons = detail.buttons.inlineButtons("ACTION")
    )

    private fun FlowMessageContext<ResourcesFlowState>.retryServerPrompt(
        step: ResourcesStep,
        prompt: ResourcesPromptModel,
        userMessageId: Int
    ): FlowResult<ResourcesFlowState> {
        val cleanup = state.payload.cleanupPromptMessages()
        return retryPrompt(
            targetStep = ResourcesStep.SERVER,
            bindingPrefix = PROMPT_MESSAGE_KEY,
            userMessageId = userMessageId,
            appendActions = { addAll(cleanup) },
            updateState = { resourcesPendingAction = state.payload.resourcesPendingAction }
        ) {
            buildPromptMessage(step, prompt, locale)
        }
    }

    private fun showHistory(
        context: FlowCallbackContext<ResourcesFlowState>,
        state: ResourcesFlowState,
        callbackQuery: CallbackQuery
    ): FlowResult<ResourcesFlowState>? {
        val serverId = state.selectedServerId ?: return null
        state.showHistory = true
        return result(context, serverId, state, callbackQuery)
    }

    private fun result(
        context: FlowCallbackContext<ResourcesFlowState>,
        serverId: Int,
        state: ResourcesFlowState,
        callbackQuery: CallbackQuery
    ): FlowResult<ResourcesFlowState> {
        val detail = viewService.buildServer(
            context.user,
            serverId,
            includeHistory = state.showHistory,
            locale = context.locale
        )
        return FlowResult(
            stepKey = ResourcesStep.SERVER.key,
            payload = state,
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildServerMessage(detail)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun quickAmountOperation(
        context: FlowCallbackContext<ResourcesFlowState>,
        state: ResourcesFlowState,
        serverId: Int,
        adjustType: AdjustType,
        callbackQuery: CallbackQuery
    ): FlowResult<ResourcesFlowState>? {
        val userEntity = userService.getUser(context.user.id) ?: return null
        val settings = userEntity.profile?.settings ?: return null
        val quickEnabled = settings.resourcesQuickChange
        val cbEnabled = settings.resourcesCb

        if (!quickEnabled) {
            return FlowResult(
                stepKey = ResourcesStep.SERVER.key,
                payload = state,
                actions = listOf(
                    AnswerCallbackAction(
                        callbackQueryId = callbackQuery.id,
                        text = i18nService.i18n(
                            "flow.resources.error.quick_disabled",
                            context.locale,
                            "Включи быстрый учёт ресурсов в настройках, чтобы пользоваться этой кнопкой."
                        ),
                        showAlert = true
                    )
                )
            )
        }

        if ((adjustType == AdjustType.ADD_CB || adjustType == AdjustType.REMOVE_CB) && !cbEnabled) {
            return buildFlowResult(
                ResourcesStep.SERVER,
                state,
                listOf(
                    AnswerCallbackAction(
                        callbackQueryId = callbackQuery.id,
                        text = i18nService.i18n(
                            "flow.resources.error.cb_disabled",
                            context.locale,
                            "Включи учёт КБ в настройках, чтобы изменить значение."
                        ),
                        showAlert = true
                    )
                )
            )
        }

        resourcesService.applyOperation(
            context.user,
            serverId,
            Adjust(adjustType, 1)
        )
        return result(context, serverId, state, callbackQuery)
    }

    private fun applyOperation(
        context: FlowCallbackContext<ResourcesFlowState>,
        state: ResourcesFlowState,
        callbackQuery: CallbackQuery,
        block: () -> Unit
    ): FlowResult<ResourcesFlowState>? {
        val serverId = state.selectedServerId ?: return null
        block()
        val detail = viewService.buildServer(
            context.user,
            serverId,
            includeHistory = state.showHistory,
            locale = context.locale
        )
        return FlowResult(
            stepKey = ResourcesStep.SERVER.key,
            payload = state.apply { resourcesPendingAction = null },
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildServerMessage(detail)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }


    private fun buildOverviewMessage(overview: ResourcesOverviewModel): FlowMessage = key.buildMessage(
        step = ResourcesStep.MAIN,
        model = overview,
        inlineButtons = overview.buttons.inlineButtons("SERVER")
    )

    private fun promptButtons(locale: Locale): List<FlowInlineButton> =
        listOf(
            key.cancelPromptButton(
                text = i18nService.i18n("flow.button.cancel", locale, "❌Отмена")
            )
        )

    private fun List<Button>.inlineButtons(action: String): List<FlowInlineButton> =
        this.map { button -> buildFlowButton(button, action) }

    private fun buildFlowButton(
        button: Button,
        action: String
    ): FlowInlineButton = FlowInlineButton(
        text = button.label,
        payload = FlowCallbackPayload(FlowKeys.RESOURCES.value, "$action:${button.action}"),
        row = button.row,
        col = button.col
    )


    private fun parseCallback(data: String): Pair<String, String?> {
        return if (data.contains(':')) {
            val split = data.split(':', limit = 2)
            split[0] to split[1]
        } else {
            data to null
        }
    }

    private fun buildFlowResult(step: ResourcesStep, payload: ResourcesFlowState, actions: List<FlowAction>) = FlowResult(
        stepKey = step.key,
        payload = payload,
        actions = actions
    )

    companion object {
        private val AMOUNT_PROMPT_ACTIONS = mapOf(
            "PROMPT_ADD_DRAADOR" to AmountActionType.ADD_DRAADOR,
            "PROMPT_SELL_DRAADOR" to AmountActionType.SELL_DRAADOR,
            "PROMPT_SEND_DRAADOR" to AmountActionType.SEND_DRAADOR,
            "PROMPT_RECEIVE_DRAADOR" to AmountActionType.RECEIVE_DRAADOR,
            "PROMPT_ADD_VOID" to AmountActionType.ADD_VOID,
            "PROMPT_REMOVE_VOID" to AmountActionType.REMOVE_VOID,
            "PROMPT_ADD_CB" to AmountActionType.ADD_CB,
            "PROMPT_REMOVE_CB" to AmountActionType.REMOVE_CB,
        )

        private val QUICK_ADJUST_ACTIONS = mapOf(
            "QUICK_INCREMENT_DRAADOR" to AdjustType.ADD_DRAADOR,
            "QUICK_DECREMENT_DRAADOR" to AdjustType.SELL_DRAADOR,
            "QUICK_RECEIVE_DRAADOR" to AdjustType.RECEIVE_DRAADOR,
            "QUICK_SEND_DRAADOR" to AdjustType.SEND_DRAADOR,
            "QUICK_INCREMENT_VOID" to AdjustType.ADD_VOID,
            "QUICK_DECREMENT_VOID" to AdjustType.REMOVE_VOID,
            "QUICK_INCREMENT_CB" to AdjustType.ADD_CB,
            "QUICK_DECREMENT_CB" to AdjustType.REMOVE_CB,
        )

        private const val MAIN_MESSAGE_KEY = "resources_main_message"
        private const val PROMPT_MESSAGE_KEY = "resources_prompt_message"
    }
}

data class ResourcesOverviewModel(
    val summaries: List<OverviewSummary>,
    val buttons: List<Button>,
)

data class OverviewSummary(
    val id: Int,
    val statusIcon: String,
    val main: Boolean,
    val exchange: String?,
    val draadorCount: Int,
    val balanceLabel: String,
    val voidCount: Int,
    val cbEnabled: Boolean,
    val cbCount: Int,
)

data class ResourcesServerModel(
    val detail: ServerDetail
)

data class ServerDetail(
    val dto: ServerResourceDto,
    val history: List<String> = emptyList(),
    val buttons: List<Button>,
)

data class ResourcesPromptModel(
    val text: String,
    val notes: List<String> = emptyList()
)

data class Button(
    val label: String,
    val action: String,
    val row: Int,
    val col: Int,
)
