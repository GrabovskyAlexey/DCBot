package ru.grabovsky.dungeoncrusherbot.strategy.flow.debt

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.entity.DebtDirection
import ru.grabovsky.dungeoncrusherbot.entity.DebtResourceType
import ru.grabovsky.dungeoncrusherbot.service.interfaces.CreateDebtRequest
import ru.grabovsky.dungeoncrusherbot.service.interfaces.DebtService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.*
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.buildMessage
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cancelPrompt
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cancelPromptButton
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cleanupPromptMessages
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.retryPrompt
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.startPrompt

@Component
class DebtFlow(
    private val debtService: DebtService,
    private val serverService: ServerService,
    private val i18nService: I18nService,
    private val debtViewService: DebtViewService,
) : FlowHandler<DebtFlowState> {

    override val key: FlowKey = FlowKeys.DEBT
    override val payloadType: Class<DebtFlowState> = DebtFlowState::class.java

    override fun start(context: FlowStartContext): FlowResult<DebtFlowState> {
        val overview = debtViewService.buildOverview(context.user.id, context.locale)
        return FlowResult(
            stepKey = DebtStep.MAIN.key,
            payload = DebtFlowState(),
            actions = listOf(
                SendMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = debtViewService.buildMainMessage(context.locale, overview)
                )
            )
        )
    }

    override fun onMessage(context: FlowContext<DebtFlowState>, message: Message): FlowResult<DebtFlowState>? {
        val creation = context.state.payload.creation
        if (creation != null) {
            return when (creation.phase) {
                DebtCreationPhase.AMOUNT -> handleAmountInput(context, message)
                DebtCreationPhase.NAME -> handleNameInput(context, message)
                else -> null
            }
        }
        if (context.state.payload.editDebtId != null) {
            return handleEditAmountInput(context, message)
        }
        return null
    }

    override fun onCallback(
        context: FlowContext<DebtFlowState>,
        callbackQuery: CallbackQuery,
        data: String
    ): FlowResult<DebtFlowState>? {
        val (command, argument) = parseCallback(data)
        return when (command) {
            "ACTION" -> handleAction(context, callbackQuery, argument)
            "DIRECTION" -> argument?.let { selectDirection(context, callbackQuery, it) }
            "SERVER" -> argument?.toIntOrNull()?.let { selectServer(context, callbackQuery, it) }
            "RESOURCE" -> argument?.let { selectResource(context, callbackQuery, it) }
            "REMOVE" -> argument?.toLongOrNull()?.let { removeDebt(context, callbackQuery, it) }
            "EDIT" -> argument?.toLongOrNull()?.let { startEdit(context, callbackQuery, it) }
            "PROMPT" -> handlePromptCallback(context, callbackQuery, argument)
            else -> null
        }
    }

    private fun handleAction(
        context: FlowContext<DebtFlowState>,
        callbackQuery: CallbackQuery,
        action: String?
    ): FlowResult<DebtFlowState>? =
        when (action) {
            "ADD" -> startCreation(context, callbackQuery)
            "CANCEL" -> cancelCreation(context, callbackQuery)
            "REMOVE_MENU" -> showRemoveMenu(context, callbackQuery)
            "EDIT_MENU" -> showEditMenu(context, callbackQuery)
            "BACK" -> showMain(context, callbackQuery)
            else -> null
        }

    private fun selectDirection(
        context: FlowContext<DebtFlowState>,
        callbackQuery: CallbackQuery,
        action: String
    ): FlowResult<DebtFlowState> {
        val direction = DebtDirection.valueOf(action)
        val state = context.state.payload.apply {
            creation = DebtCreationState(
                direction = direction,
                phase = DebtCreationPhase.SERVER,
            )
        }
        val servers = serverService.getAllServers()
        return FlowResult(
            stepKey = DebtStep.CREATE.key,
            payload = state,
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = debtViewService.buildCreateMessage(context.locale, state.creation!!, DebtCreationPhase.SERVER, servers)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun selectServer(
        context: FlowContext<DebtFlowState>,
        callbackQuery: CallbackQuery,
        serverId: Int
    ): FlowResult<DebtFlowState> {
        val creation = context.state.payload.creation ?: return startCreation(context, callbackQuery)
        val servers = serverService.getAllServers()
        if (servers.none { it.id == serverId }) {
            return FlowResult(
                stepKey = DebtStep.CREATE.key,
                payload = context.state.payload,
                actions = listOf(
                    AnswerCallbackAction(
                        callbackQueryId = callbackQuery.id,
                        text = i18nService.i18n("flow.debt.error.server_not_found", context.locale, "Сервер не найден"),
                        showAlert = true
                    )
                )
            )
        }
        creation.serverId = serverId
        creation.phase = DebtCreationPhase.RESOURCE
        return FlowResult(
            stepKey = DebtStep.CREATE.key,
            payload = context.state.payload,
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = debtViewService.buildCreateMessage(context.locale, creation, DebtCreationPhase.RESOURCE, servers)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun selectResource(
        context: FlowContext<DebtFlowState>,
        callbackQuery: CallbackQuery,
        resourceName: String
    ): FlowResult<DebtFlowState>? {
        val creation = context.state.payload.creation ?: return startCreation(context, callbackQuery)
        val resource = runCatching { DebtResourceType.valueOf(resourceName) }.getOrNull()
            ?: return null
        creation.resourceType = resource
        creation.phase = DebtCreationPhase.AMOUNT
        val cleanup = context.state.payload.cleanupPromptMessages()
        val servers = serverService.getAllServers()
        return context.startPrompt(
            targetStep = DebtStep.CREATE,
            bindingPrefix = PROMPT_BINDING_PREFIX,
            callbackQuery = callbackQuery,
            updateState = { creation.phase = DebtCreationPhase.AMOUNT },
            appendActions = {
                addAll(cleanup)
                add(
                    EditMessageAction(
                        bindingKey = MAIN_MESSAGE_KEY,
                        message = debtViewService.buildCreateMessage(context.locale, creation, DebtCreationPhase.AMOUNT, servers)
                    )
                )
            }
        ) {
            val view = debtViewService.run { creation.view(context.locale) }
            debtViewService.buildAmountPrompt(context.locale, view)
        }
    }

    private fun handleAmountInput(
        context: FlowContext<DebtFlowState>,
        message: Message
    ): FlowResult<DebtFlowState>? {
        val creation = context.state.payload.creation ?: return null
        val amount = message.text?.trim()?.toIntOrNull()
        if (amount == null || amount <= 0) {
            return context.retryPrompt(
                targetStep = DebtStep.CREATE,
                bindingPrefix = PROMPT_BINDING_PREFIX,
                userMessageId = message.messageId
            ) {
                val view = debtViewService.run { creation.view(context.locale) }
                debtViewService.buildAmountPrompt(context.locale, view, invalid = true)
            }
        }
        creation.amount = amount
        creation.phase = DebtCreationPhase.NAME
        val cleanup = context.state.payload.cleanupPromptMessages()
        val servers = serverService.getAllServers()
        return context.retryPrompt(
            targetStep = DebtStep.CREATE,
            bindingPrefix = PROMPT_BINDING_PREFIX,
            userMessageId = message.messageId,
            updateState = { creation.phase = DebtCreationPhase.NAME },
            appendActions = {
                addAll(cleanup)
                add(
                    EditMessageAction(
                        bindingKey = MAIN_MESSAGE_KEY,
                        message = debtViewService.buildCreateMessage(context.locale, creation, DebtCreationPhase.NAME, servers)
                    )
                )
            },
        ) {
            debtViewService.buildNamePrompt(context.locale, creation)
        }
    }

    private fun handleNameInput(
        context: FlowContext<DebtFlowState>,
        message: Message
    ): FlowResult<DebtFlowState>? {
        val creation = context.state.payload.creation ?: return null
        val name = message.text?.trim().orEmpty()
        if (name.isBlank()) {
            return context.retryPrompt(
                targetStep = DebtStep.CREATE,
                bindingPrefix = PROMPT_BINDING_PREFIX,
                userMessageId = message.messageId
            ) {
                debtViewService.buildNamePrompt(context.locale, creation, invalid = true)
            }
        }
        creation.counterpartyName = name
        val overview = createDebt(context, creation)
        return resultWithMain(context, overview, deleteUserMessageId = message.messageId)
    }

    private fun handleEditAmountInput(
        context: FlowContext<DebtFlowState>,
        message: Message
    ): FlowResult<DebtFlowState>? {
        val debtId = context.state.payload.editDebtId ?: return null
        val overview = debtViewService.buildOverview(context.user.id, context.locale)
        val debt = (overview.oweMe + overview.iOwe).firstOrNull { it.id == debtId }
        if (debt == null) {
            return resultWithMain(context, overview, deleteUserMessageId = message.messageId)
        }

        val amount = message.text?.trim()?.toIntOrNull()
        if (amount == null || amount <= 0) {
            return context.retryPrompt(
                targetStep = DebtStep.MAIN,
                bindingPrefix = EDIT_PROMPT_BINDING_PREFIX,
                userMessageId = message.messageId
            ) {
                val view = debtViewService.run { debt.toCreationView() }
                debtViewService.buildAmountPrompt(context.locale, view, invalid = true)
            }
        }

        debtService.updateAmount(context.user.id, debtId, amount)
        val updatedOverview = debtViewService.buildOverview(context.user.id, context.locale)
        return resultWithMain(context, updatedOverview, deleteUserMessageId = message.messageId)
    }

    private fun removeDebt(
        context: FlowContext<DebtFlowState>,
        callbackQuery: CallbackQuery,
        debtId: Long
    ): FlowResult<DebtFlowState> {
        val removed = debtService.remove(context.user.id, debtId)
        if (!removed) {
            return FlowResult(
                stepKey = context.state.stepKey,
                payload = context.state.payload,
                actions = listOf(
                    AnswerCallbackAction(
                        callbackQueryId = callbackQuery.id,
                        text = i18nService.i18n("flow.debt.error.not_found", context.locale, "Запись не найдена"),
                        showAlert = true
                    )
                )
            )
        }
        val overview = debtViewService.buildOverview(context.user.id, context.locale)
        return FlowResult(
            stepKey = DebtStep.MAIN.key,
            payload = DebtFlowState(),
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = debtViewService.buildMainMessage(context.locale, overview)
                ),
                AnswerCallbackAction(
                    callbackQueryId = callbackQuery.id,
                    text = i18nService.i18n("flow.debt.success.removed", context.locale, "Долг удалён")
                )
            )
        )
    }

    private fun startCreation(
        context: FlowContext<DebtFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<DebtFlowState> {
        val payload = DebtFlowState(creation = DebtCreationState())
        val servers = serverService.getAllServers()
        return FlowResult(
            stepKey = DebtStep.CREATE.key,
            payload = payload,
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = debtViewService.buildCreateMessage(context.locale, payload.creation!!, DebtCreationPhase.DIRECTION, servers)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun cancelCreation(
        context: FlowContext<DebtFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<DebtFlowState> {
        val overview = debtViewService.buildOverview(context.user.id, context.locale)
        val cleanup = context.state.payload.cleanupPromptMessages()
        return FlowResult(
            stepKey = DebtStep.MAIN.key,
            payload = DebtFlowState(),
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = debtViewService.buildMainMessage(context.locale, overview)
                ),
                AnswerCallbackAction(callbackQuery.id)
            ) + cleanup
        )
    }

    private fun showMain(
        context: FlowContext<DebtFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<DebtFlowState> {
        val overview = debtViewService.buildOverview(context.user.id, context.locale)
        return FlowResult(
            stepKey = DebtStep.MAIN.key,
            payload = DebtFlowState(),
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = debtViewService.buildMainMessage(context.locale, overview)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun startEdit(
        context: FlowContext<DebtFlowState>,
        callbackQuery: CallbackQuery,
        debtId: Long
    ): FlowResult<DebtFlowState> {
        val overview = debtViewService.buildOverview(context.user.id, context.locale)
        val debt = (overview.oweMe + overview.iOwe).firstOrNull { it.id == debtId }
        if (debt == null) {
            return FlowResult(
                stepKey = DebtStep.MAIN.key,
                payload = context.state.payload,
                actions = listOf(
                    AnswerCallbackAction(
                        callbackQueryId = callbackQuery.id,
                        text = i18nService.i18n("flow.debt.error.not_found", context.locale, "Запись не найдена"),
                        showAlert = true
                    )
                )
            )
        }
        val cleanup = context.state.payload.cleanupPromptMessages()
        return context.startPrompt(
            targetStep = DebtStep.MAIN,
            bindingPrefix = EDIT_PROMPT_BINDING_PREFIX,
            callbackQuery = callbackQuery,
            updateState = {
                editDebtId = debtId
                creation = null
            },
            appendActions = { addAll(cleanup) }
        ) {
            val view = debtViewService.run { debt.toCreationView() }
            debtViewService.buildAmountPrompt(context.locale, view)
        }
    }

    private fun showEditMenu(
        context: FlowContext<DebtFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<DebtFlowState> =
        showMenu(
            context = context,
            callbackQuery = callbackQuery,
            buttonsBuilder = { overview -> debtViewService.buildItemButtons(context.locale, overview, "✏️", "EDIT") },
            emptyFallback = "Записей нет"
        )

    private fun showRemoveMenu(
        context: FlowContext<DebtFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<DebtFlowState> =
        showMenu(
            context = context,
            callbackQuery = callbackQuery,
            buttonsBuilder = { overview -> debtViewService.buildItemButtons(context.locale, overview, "❌", "REMOVE") },
            emptyFallback = "Запись не найдена",
            emptyStepKey = context.state.stepKey
        )

    private fun handlePromptCallback(
        context: FlowContext<DebtFlowState>,
        callbackQuery: CallbackQuery,
        argument: String?
    ): FlowResult<DebtFlowState>? {
        if (argument != "CANCEL") {
            return null
        }
        val cancelResult = context.cancelPrompt(
            targetStep = DebtStep.MAIN,
            callbackQuery = callbackQuery,
            updateState = {
                creation = null
                editDebtId = null
            }
        )
        val overview = debtViewService.buildOverview(context.user.id, context.locale)
        val actions = cancelResult.actions.toMutableList().apply {
            add(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = debtViewService.buildMainMessage(context.locale, overview)
                )
            )
        }
        return cancelResult.copy(
            stepKey = DebtStep.MAIN.key,
            payload = DebtFlowState(),
            actions = actions
        )
    }

    private fun resultWithMain(
        context: FlowContext<DebtFlowState>,
        overview: DebtOverviewModel,
        deleteUserMessageId: Int? = null
    ): FlowResult<DebtFlowState> {
        val actions = mutableListOf<FlowAction>()
        actions += context.state.payload.cleanupPromptMessages()
        if (deleteUserMessageId != null) {
            actions += DeleteMessageIdAction(deleteUserMessageId)
        }
        actions += EditMessageAction(
            bindingKey = MAIN_MESSAGE_KEY,
            message = debtViewService.buildMainMessage(context.locale, overview)
        )
        return FlowResult(
            stepKey = DebtStep.MAIN.key,
            payload = DebtFlowState(),
            actions = actions
        )
    }

    private fun showMenu(
        context: FlowContext<DebtFlowState>,
        callbackQuery: CallbackQuery,
        buttonsBuilder: (DebtOverviewModel) -> List<FlowInlineButton>,
        emptyFallback: String,
        emptyStepKey: String = DebtStep.MAIN.key
    ): FlowResult<DebtFlowState> {
        val overview = debtViewService.buildOverview(context.user.id, context.locale)
        val debts = overview.oweMe + overview.iOwe
        if (debts.isEmpty()) {
            return FlowResult(
                stepKey = emptyStepKey,
                payload = context.state.payload,
                actions = listOf(
                    AnswerCallbackAction(
                        callbackQueryId = callbackQuery.id,
                        text = i18nService.i18n("flow.debt.error.not_found", context.locale, emptyFallback),
                        showAlert = true
                    )
                )
            )
        }
        return FlowResult(
            stepKey = DebtStep.MAIN.key,
            payload = context.state.payload,
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = key.buildMessage(
                        step = DebtStep.MAIN,
                        model = overview,
                        inlineButtons = buttonsBuilder(overview)
                    )
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun createDebt(context: FlowContext<DebtFlowState>, creation: DebtCreationState): DebtOverviewModel {
        val request = CreateDebtRequest(
            userId = context.user.id,
            direction = creation.direction!!,
            serverId = creation.serverId!!,
            resourceType = creation.resourceType!!,
            amount = creation.amount!!,
            counterpartyName = creation.counterpartyName!!.trim()
        )
        debtService.create(request)
        return debtViewService.buildOverview(context.user.id, context.locale)
    }

    companion object {
        private const val MAIN_MESSAGE_KEY = "debt_main"
        private const val PROMPT_BINDING_PREFIX = "debt_prompt"
        private const val EDIT_PROMPT_BINDING_PREFIX = "debt_edit_prompt"
    }
}
