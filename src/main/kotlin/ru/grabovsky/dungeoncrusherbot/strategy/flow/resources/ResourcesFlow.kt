package ru.grabovsky.dungeoncrusherbot.strategy.flow.resources

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.service.interfaces.*
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ResourceOperation.*
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerResourceDto
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.*
import ru.grabovsky.dungeoncrusherbot.strategy.flow.resources.ResourcesPendingAction.*
import ru.grabovsky.dungeoncrusherbot.util.FlowUtils
import java.util.Locale
import java.util.UUID

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
            "PROMPT" -> handlePromptCallback(payload, argument, callbackQuery)
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
                message = FlowMessage(
                    flowKey = key,
                    stepKey = ResourcesStep.SERVER.key,
                    model = ResourcesServerModel(detail),
                    inlineButtons = detail.buttons.inlineButtons("ACTION")
                )
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
            "PROMPT_ADD_DRAADOR" -> enterAmountPrompt(context, state, serverId, AmountActionType.ADD_DRAADOR, callbackQuery)
            "PROMPT_SELL_DRAADOR" -> enterAmountPrompt(context, state, serverId, AmountActionType.SELL_DRAADOR, callbackQuery)
            "PROMPT_SEND_DRAADOR" -> enterAmountPrompt(context, state, serverId, AmountActionType.SEND_DRAADOR, callbackQuery)
            "PROMPT_RECEIVE_DRAADOR" -> enterAmountPrompt(context, state, serverId, AmountActionType.RECEIVE_DRAADOR, callbackQuery)
            "PROMPT_ADD_VOID" -> enterAmountPrompt(context, state, serverId, AmountActionType.ADD_VOID, callbackQuery)
            "PROMPT_REMOVE_VOID" -> enterAmountPrompt(context, state, serverId, AmountActionType.REMOVE_VOID, callbackQuery)
            "PROMPT_ADD_CB" -> enterAmountPrompt(context, state, serverId, AmountActionType.ADD_CB, callbackQuery)
            "PROMPT_REMOVE_CB" -> enterAmountPrompt(context, state, serverId, AmountActionType.REMOVE_CB, callbackQuery)
            "PROMPT_ADD_EXCHANGE" -> enterExchangePrompt(context, state, serverId, callbackQuery)
            "PROMPT_ADD_NOTE" -> enterAddNotePrompt(context, state, serverId, callbackQuery)
            "PROMPT_REMOVE_NOTE" -> enterRemoveNotePrompt(context, state, serverId, callbackQuery)
            "QUICK_INCREMENT_DRAADOR" -> quickAmountOperation(context, state, serverId, AdjustType.ADD_DRAADOR, callbackQuery)
            "QUICK_DECREMENT_DRAADOR" -> quickAmountOperation(context, state, serverId, AdjustType.SELL_DRAADOR, callbackQuery)
            "QUICK_INCREMENT_VOID" -> quickAmountOperation(context, state, serverId, AdjustType.ADD_VOID, callbackQuery)
            "QUICK_DECREMENT_VOID" -> quickAmountOperation(context, state, serverId, AdjustType.REMOVE_VOID, callbackQuery)
            "QUICK_INCREMENT_CB" -> quickAmountOperation(context, state, serverId, AdjustType.ADD_CB, callbackQuery)
            "QUICK_DECREMENT_CB" -> quickAmountOperation(context, state, serverId, AdjustType.REMOVE_CB, callbackQuery)
            "QUICK_RECEIVE_DRAADOR" -> quickAmountOperation(context, state, serverId, AdjustType.RECEIVE_DRAADOR, callbackQuery)
            "QUICK_SEND_DRAADOR" -> quickAmountOperation(context, state, serverId, AdjustType.SEND_DRAADOR, callbackQuery)
            else -> null
        }
    }

    private fun handlePromptCallback(
        state: ResourcesFlowState,
        argument: String?,
        callbackQuery: CallbackQuery
    ): FlowResult<ResourcesFlowState>? {
        return when (argument) {
            "CANCEL" -> cancelPrompt(state, callbackQuery)
            else -> null
        }
    }

    private fun handleAmountInput(
        context: FlowMessageContext<ResourcesFlowState>,
        message: Message,
        pending: Amount
    ): FlowResult<ResourcesFlowState> {
        val amount = message.text?.toIntOrNull()
        if (amount == null || amount <= 0) {
            val prompt = promptBuilder.amountPrompt(context.locale, pending.operation, invalid = true)
            val promptBinding = nextPromptBinding()
            val state = context.state.payload
            state.promptBindings.add(promptBinding)
            return FlowResult(
                stepKey = ResourcesStep.SERVER.key,
                payload = state,
                actions = listOf(
                    SendMessageAction(
                        bindingKey = promptBinding,
                        message = FlowMessage(
                            flowKey = key,
                            stepKey = ResourcesStep.PROMPT_AMOUNT.key,
                            model = prompt,
                            inlineButtons = promptButtons(context.locale)
                        )
                    ),
                    DeleteMessageIdAction(message.messageId)
                )
            )
        }

        val operation = Adjust(pending.operation.toAdjustType(), amount)
        resourcesService.applyOperation(context.user, pending.serverId, operation)
        return finalizePrompt(context, pending.serverId, message.messageId)
    }

    private fun handleExchangeInput(
        context: FlowMessageContext<ResourcesFlowState>,
        message: Message,
        pending: Exchange
    ): FlowResult<ResourcesFlowState> {
        val value = message.text?.trim().orEmpty()
        if (value.isEmpty()) {
            val prompt = promptBuilder.exchangePrompt(context.locale, invalid = true)
            return getFlowResultForInput(context, prompt, message)
        }

        resourcesService.applyOperation(context.user, pending.serverId, SetExchange(value))
        return finalizePrompt(context, pending.serverId, message.messageId)
    }

    private fun handleAddNoteInput(
        context: FlowMessageContext<ResourcesFlowState>,
        message: Message,
        pending: AddNote
    ): FlowResult<ResourcesFlowState> {
        val value = message.text?.trim().orEmpty()
        if (value.isEmpty()) {
            val prompt = promptBuilder.addNotePrompt(context.locale, invalid = true)
            return getFlowResultForInput(context, prompt, message)
        }

        val user = userService.getUser(context.user.id) ?: return finalizePrompt(context, pending.serverId, message.messageId)
        val notes = user.notes
        if (notes.size >= 20) {
            notes.removeFirst()
        }
        notes.add(value)
        userService.saveUser(user)
        return finalizePrompt(context, pending.serverId, message.messageId)
    }

    private fun handleRemoveNoteInput(
        context: FlowMessageContext<ResourcesFlowState>,
        message: Message,
        pending: RemoveNote
    ): FlowResult<ResourcesFlowState> {
        val index = message.text?.toIntOrNull()
        val user = userService.getUser(context.user.id) ?: return finalizePrompt(context, pending.serverId, message.messageId)
        val notes = user.notes
        if (index == null || index <= 0 || index > notes.size) {
            val prompt = promptBuilder.removeNotePrompt(context.locale, notes, invalid = true)
            return getFlowResultForInput(context, prompt, message)
        }

        user.notes.removeAt(index - 1)
        userService.saveUser(user)
        return finalizePrompt(context, pending.serverId, message.messageId)
    }

    private fun getFlowResultForInput(
        context: FlowMessageContext<ResourcesFlowState>,
        prompt: ResourcesPromptModel,
        message: Message
    ): FlowResult<ResourcesFlowState> {
        val promptBinding = nextPromptBinding()
        val state = context.state.payload
        state.promptBindings.add(promptBinding)
        return FlowResult(
            stepKey = ResourcesStep.SERVER.key,
            payload = state,
            actions = listOf(
                SendMessageAction(
                    bindingKey = promptBinding,
                    message = FlowMessage(
                        flowKey = key,
                        stepKey = ResourcesStep.PROMPT_TEXT.key,
                        model = prompt,
                        inlineButtons = promptButtons(context.locale)
                    )
                ),
                DeleteMessageIdAction(message.messageId)
            )
        )
    }

    private fun finalizePrompt(
        context: FlowMessageContext<ResourcesFlowState>,
        serverId: Int,
        userMessageId: Int?
    ): FlowResult<ResourcesFlowState> {
        val cleanupActions = FlowUtils.cleanupPromptActions(context.state.payload.promptBindings)
        userMessageId?.let { cleanupActions += DeleteMessageIdAction(it) }
        val state = context.state.payload
        clearState(state)
        val detail = viewService.buildServer(context.user, serverId, includeHistory = state.showHistory, locale = context.locale)
        cleanupActions += EditMessageAction(
            bindingKey = MAIN_MESSAGE_KEY,
            message = FlowMessage(
                flowKey = key,
                stepKey = ResourcesStep.SERVER.key,
                model = ResourcesServerModel(detail),
                inlineButtons = detail.buttons.inlineButtons("ACTION")
            )
        )
        return buildFlowResult(ResourcesStep.SERVER, state, cleanupActions)
    }

    private fun cancelPrompt(
        state: ResourcesFlowState,
        callbackQuery: CallbackQuery
    ): FlowResult<ResourcesFlowState> {
        val cleanupActions = FlowUtils.cleanupPromptActions(state.promptBindings)
        clearState(state)
        cleanupActions += AnswerCallbackAction(callbackQuery.id)
        return FlowResult(
            stepKey = if (state.selectedServerId != null) ResourcesStep.SERVER.key else ResourcesStep.MAIN.key,
            payload = state,
            actions = cleanupActions
        )
    }

    private fun clearState(state: ResourcesFlowState) {
        state.resourcesPendingAction = null
        state.promptBindings.clear()
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
                    message = FlowMessage(
                        flowKey = key,
                        stepKey = ResourcesStep.SERVER.key,
                        model = ResourcesServerModel(detail),
                        inlineButtons = detail.buttons.inlineButtons("ACTION")
                    )
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
        val quickEnabled = userEntity.settings.resourcesQuickChange
        val cbEnabled = userEntity.settings.resourcesCb

        if (!quickEnabled) {
            return FlowResult(
                stepKey = ResourcesStep.SERVER.key,
                payload = state,
                actions = listOf(
                    AnswerCallbackAction(
                        callbackQueryId = callbackQuery.id,
                        text = i18nService.i18n("flow.resources.error.quick_disabled", context.locale, "Включи быстрый учёт ресурсов в настройках, чтобы пользоваться этой кнопкой."),
                        showAlert = true
                    )
                )
            )
        }

        if ((adjustType == AdjustType.ADD_CB || adjustType == AdjustType.REMOVE_CB) && !cbEnabled) {
            return buildFlowResult(ResourcesStep.SERVER, state, listOf(
                AnswerCallbackAction(
                    callbackQueryId = callbackQuery.id,
                    text = i18nService.i18n("flow.resources.error.cb_disabled", context.locale, "Включи учёт КБ в настройках, чтобы изменить значение."),
                    showAlert = true
                )
            ))
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
        val detail = viewService.buildServer(context.user, serverId, includeHistory = state.showHistory, locale = context.locale)
        return FlowResult(
            stepKey = ResourcesStep.SERVER.key,
            payload = state.apply { resourcesPendingAction = null },
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = FlowMessage(
                        flowKey = key,
                        stepKey = ResourcesStep.SERVER.key,
                        model = ResourcesServerModel(detail),
                        inlineButtons = detail.buttons.inlineButtons("ACTION")
                    )
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun enterAmountPrompt(
        context: FlowCallbackContext<ResourcesFlowState>,
        state: ResourcesFlowState,
        serverId: Int,
        type: AmountActionType,
        callbackQuery: CallbackQuery
    ): FlowResult<ResourcesFlowState> {
        val prompt = promptBuilder.amountPrompt(context.locale, type)
        val promptBinding = nextPromptBinding()
        state.promptBindings.add(promptBinding)
        state.resourcesPendingAction = Amount(type, serverId)
        return FlowResult(
            stepKey = ResourcesStep.SERVER.key,
            payload = state,
            actions = listOf(
                SendMessageAction(
                    bindingKey = promptBinding,
                    message = buildPromptMessage(ResourcesStep.PROMPT_AMOUNT, prompt, context)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun enterExchangePrompt(
        context: FlowCallbackContext<ResourcesFlowState>,
        state: ResourcesFlowState,
        serverId: Int,
        callbackQuery: CallbackQuery
    ): FlowResult<ResourcesFlowState> {
        val prompt = promptBuilder.exchangePrompt(context.locale, invalid = false)
        val promptBinding = nextPromptBinding()
        state.promptBindings.add(promptBinding)
        state.resourcesPendingAction = Exchange(serverId)
        return FlowResult(
            stepKey = ResourcesStep.SERVER.key,
            payload = state,
            actions = listOf(
                SendMessageAction(
                    bindingKey = promptBinding,
                    message = buildPromptMessage(ResourcesStep.PROMPT_TEXT, prompt, context)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun enterAddNotePrompt(
        context: FlowCallbackContext<ResourcesFlowState>,
        state: ResourcesFlowState,
        serverId: Int,
        callbackQuery: CallbackQuery
    ): FlowResult<ResourcesFlowState> {
        val prompt = promptBuilder.addNotePrompt(context.locale, invalid = false)
        val promptBinding = nextPromptBinding()
        state.promptBindings.add(promptBinding)
        state.resourcesPendingAction = AddNote(serverId)
        return buildFlowResult(ResourcesStep.SERVER, state, listOf(
            SendMessageAction(
                bindingKey = promptBinding,
                message = buildPromptMessage(ResourcesStep.PROMPT_TEXT, prompt, context)
            ),
            AnswerCallbackAction(callbackQuery.id)
        ))
    }

    private fun enterRemoveNotePrompt(
        context: FlowCallbackContext<ResourcesFlowState>,
        state: ResourcesFlowState,
        serverId: Int,
        callbackQuery: CallbackQuery
    ): FlowResult<ResourcesFlowState> {
        val user = userService.getUser(context.user.id)
        val notes = user?.notes ?: emptyList()
        val prompt = promptBuilder.removeNotePrompt(context.locale, notes, invalid = false)
        val promptBinding = nextPromptBinding()
        state.promptBindings.add(promptBinding)
        state.resourcesPendingAction = RemoveNote(serverId)
        return FlowResult(
            stepKey = ResourcesStep.SERVER.key,
            payload = state,
            actions = listOf(
                SendMessageAction(
                    bindingKey = promptBinding,
                    message = buildPromptMessage(ResourcesStep.PROMPT_TEXT, prompt, context)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun buildPromptMessage(
        step: ResourcesStep,
        prompt: ResourcesPromptModel,
        context: FlowCallbackContext<ResourcesFlowState>
    ): FlowMessage = FlowMessage(
        flowKey = key,
        stepKey = step.key,
        model = prompt,
        inlineButtons = promptButtons(context.locale)
    )

    private fun buildOverviewMessage(overview: ResourcesOverviewModel): FlowMessage = FlowMessage(
        flowKey = key,
        stepKey = ResourcesStep.MAIN.key,
        model = overview,
        inlineButtons = overview.buttons.inlineButtons("SERVER")
    )

    private fun promptButtons(locale: Locale): List<FlowInlineButton> =
        listOf(
            FlowInlineButton(
                text = i18nService.i18n("flow.button.cancel", locale, "❌Отмена"),
                payload = FlowCallbackPayload(key.value, "PROMPT:CANCEL"),
                row = 0,
                col = 0
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

    private fun nextPromptBinding(): String = "${PROMPT_MESSAGE_KEY}_${UUID.randomUUID()}"


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
        private const val MAIN_MESSAGE_KEY = "resources_main_message"
        private const val PROMPT_MESSAGE_KEY = "resources_prompt_message"
        private val logger = KotlinLogging.logger {}
    }
}

// 631

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