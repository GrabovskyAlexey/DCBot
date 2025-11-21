package ru.grabovsky.dungeoncrusherbot.strategy.flow.debt

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.entity.Debt
import ru.grabovsky.dungeoncrusherbot.entity.DebtDirection
import ru.grabovsky.dungeoncrusherbot.entity.DebtResourceType
import ru.grabovsky.dungeoncrusherbot.entity.Server
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
import java.util.Locale

@Component
class DebtFlow(
    private val debtService: DebtService,
    private val serverService: ServerService,
    private val i18nService: I18nService,
) : FlowHandler<DebtFlowState> {

    override val key: FlowKey = FlowKeys.DEBT
    override val payloadType: Class<DebtFlowState> = DebtFlowState::class.java

    override fun start(context: FlowStartContext): FlowResult<DebtFlowState> {
        val overview = buildOverview(context.user.id, context.locale)
        return FlowResult(
            stepKey = DebtStep.MAIN.key,
            payload = DebtFlowState(),
            actions = listOf(
                SendMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildMainMessage(context.locale, overview)
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
                    message = buildCreateMessage(context.locale, state.creation!!, DebtCreationPhase.SERVER, servers)
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
                    message = buildCreateMessage(context.locale, creation, DebtCreationPhase.RESOURCE, servers)
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
                        message = buildCreateMessage(context.locale, creation, DebtCreationPhase.AMOUNT, servers)
                    )
                )
            }
        ) {
            buildAmountPrompt(context.locale, creation)
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
                buildAmountPrompt(context.locale, creation, invalid = true)
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
                        message = buildCreateMessage(context.locale, creation, DebtCreationPhase.NAME, servers)
                    )
                )
            },
        ) {
            buildNamePrompt(context.locale, creation)
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
                buildNamePrompt(context.locale, creation, invalid = true)
            }
        }
        creation.counterpartyName = name
        val cleanup = context.state.payload.cleanupPromptMessages()
        val overview = createDebt(context, creation)
        val newState = DebtFlowState()
        val actions = mutableListOf<FlowAction>()
        actions += cleanup
        actions += DeleteMessageIdAction(message.messageId)
        actions += EditMessageAction(
            bindingKey = MAIN_MESSAGE_KEY,
            message = buildMainMessage(context.locale, overview)
        )
        return FlowResult(
            stepKey = DebtStep.MAIN.key,
            payload = newState,
            actions = actions
        )
    }

    private fun handleEditAmountInput(
        context: FlowContext<DebtFlowState>,
        message: Message
    ): FlowResult<DebtFlowState>? {
        val debtId = context.state.payload.editDebtId ?: return null
        val overview = buildOverview(context.user.id, context.locale)
        val debt = (overview.oweMe + overview.iOwe).firstOrNull { it.id == debtId }
        if (debt == null) {
            val cleanup = context.state.payload.cleanupPromptMessages()
            val actions = mutableListOf<FlowAction>()
            actions += cleanup
            actions += DeleteMessageIdAction(message.messageId)
            actions += EditMessageAction(
                bindingKey = MAIN_MESSAGE_KEY,
                message = buildMainMessage(context.locale, overview)
            )
            return FlowResult(
                stepKey = DebtStep.MAIN.key,
                payload = DebtFlowState(),
                actions = actions
            )
        }

        val amount = message.text?.trim()?.toIntOrNull()
        if (amount == null || amount <= 0) {
            return context.retryPrompt(
                targetStep = DebtStep.MAIN,
                bindingPrefix = EDIT_PROMPT_BINDING_PREFIX,
                userMessageId = message.messageId
            ) {
                buildEditAmountPrompt(context.locale, debt, invalid = true)
            }
        }

        debtService.updateAmount(context.user.id, debtId, amount)
        val updatedOverview = buildOverview(context.user.id, context.locale)
        val cleanup = context.state.payload.cleanupPromptMessages()
        val actions = mutableListOf<FlowAction>()
        actions += cleanup
        actions += DeleteMessageIdAction(message.messageId)
        actions += EditMessageAction(
            bindingKey = MAIN_MESSAGE_KEY,
            message = buildMainMessage(context.locale, updatedOverview)
        )
        return FlowResult(
            stepKey = DebtStep.MAIN.key,
            payload = DebtFlowState(),
            actions = actions
        )
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
        val overview = buildOverview(context.user.id, context.locale)
        return FlowResult(
            stepKey = DebtStep.MAIN.key,
            payload = DebtFlowState(),
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildMainMessage(context.locale, overview)
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
                    message = buildCreateMessage(context.locale, payload.creation!!, DebtCreationPhase.DIRECTION, servers)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun cancelCreation(
        context: FlowContext<DebtFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<DebtFlowState> {
        val overview = buildOverview(context.user.id, context.locale)
        val cleanup = context.state.payload.cleanupPromptMessages()
        return FlowResult(
            stepKey = DebtStep.MAIN.key,
            payload = DebtFlowState(),
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildMainMessage(context.locale, overview)
                ),
                AnswerCallbackAction(callbackQuery.id)
            ) + cleanup
        )
    }

    private fun showMain(
        context: FlowContext<DebtFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<DebtFlowState> {
        val overview = buildOverview(context.user.id, context.locale)
        return FlowResult(
            stepKey = DebtStep.MAIN.key,
            payload = DebtFlowState(),
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildMainMessage(context.locale, overview)
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
        val overview = buildOverview(context.user.id, context.locale)
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
            buildEditAmountPrompt(context.locale, debt)
        }
    }

    private fun showEditMenu(
        context: FlowContext<DebtFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<DebtFlowState>? {
        val overview = buildOverview(context.user.id, context.locale)
        val debts = overview.oweMe + overview.iOwe
        if (debts.isEmpty()) {
            return FlowResult(
                stepKey = DebtStep.MAIN.key,
                payload = context.state.payload,
                actions = listOf(
                    AnswerCallbackAction(
                        callbackQueryId = callbackQuery.id,
                        text = i18nService.i18n("flow.debt.error.not_found", context.locale, "Записей нет"),
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
                        inlineButtons = buildEditButtons(context.locale, overview)
                    )
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun showRemoveMenu(
        context: FlowContext<DebtFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<DebtFlowState> {
        val overview = buildOverview(context.user.id, context.locale)
        val debts = overview.oweMe + overview.iOwe
        if (debts.isEmpty()) {
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
        return FlowResult(
            stepKey = DebtStep.MAIN.key,
            payload = context.state.payload,
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = key.buildMessage(
                        step = DebtStep.MAIN,
                        model = overview,
                        inlineButtons = buildRemoveButtons(context.locale, overview)
                    )
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

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
        val overview = buildOverview(context.user.id, context.locale)
        val actions = cancelResult.actions.toMutableList().apply {
            add(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildMainMessage(context.locale, overview)
                )
            )
        }
        return cancelResult.copy(
            stepKey = DebtStep.MAIN.key,
            payload = DebtFlowState(),
            actions = actions
        )
    }

    private fun buildOverview(userId: Long, locale: Locale): DebtOverviewModel {
        val debts = debtService.getDebts(userId)
        val servers = serverService.getAllServers().associateBy { it.id }

        val oweMeOrdered = debts
            .filter { it.direction == DebtDirection.OWE_ME }
            .mapIndexed { index, debt -> debt to (index + 1) }
            .toMap()

        val iOweOrdered = debts
            .filter { it.direction == DebtDirection.I_OWE }
            .mapIndexed { index, debt -> debt to (oweMeOrdered.size + index + 1) }
            .toMap()

        val oweMe = oweMeOrdered.map { (debt, number) ->
            debt.toView(locale, servers[debt.serverId], number)
        }
        val iOwe = iOweOrdered.map { (debt, number) ->
            debt.toView(locale, servers[debt.serverId], number)
        }

        return DebtOverviewModel(
            oweMe = oweMe,
            iOwe = iOwe
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
        return buildOverview(context.user.id, context.locale)
    }

    private fun Debt.toView(locale: Locale, server: Server?, displayNumber: Int): DebtItemModel =
        DebtItemModel(
            id = id ?: 0,
            displayNumber = displayNumber,
            directionLabel = directionLabel(direction, locale),
            resourceLabel = resourceLabel(resourceType, locale),
            amount = amount,
            server = "$serverId",
            counterparty = counterpartyName,
        )

    private fun directionLabel(direction: DebtDirection, locale: Locale): String {
        val key = when (direction) {
            DebtDirection.OWE_ME -> "flow.debt.direction.OWE_ME"
            DebtDirection.I_OWE -> "flow.debt.direction.I_OWE"
        }
        val fallback = when (direction) {
            DebtDirection.OWE_ME -> "Вам должны"
            DebtDirection.I_OWE -> "Вы должны"
        }
        return i18nService.i18n(key, locale, fallback)
    }

    private fun resourceLabel(type: DebtResourceType, locale: Locale): String {
        val key = "flow.debt.resource.${type.name}"
        val fallback = when (type) {
            DebtResourceType.VOID -> "\uD83D\uDFE3"
            DebtResourceType.MAP -> "\uD83E\uDE86"
            DebtResourceType.CB -> "\uD83D\uDE08"
            DebtResourceType.BOTTLES -> "\uD83E\uDED9"
            DebtResourceType.CANNON -> "\uD83D\uDD2B"
        }
        return i18nService.i18n(key, locale, fallback)
    }

    private fun buildMainMessage(locale: Locale, overview: DebtOverviewModel): FlowMessage =
        key.buildMessage(
            step = DebtStep.MAIN,
            model = overview,
            inlineButtons = buildMainButtons(locale)
        )

    private fun buildMainButtons(locale: Locale): List<FlowInlineButton> =
        listOf(
            FlowInlineButton(
                text = i18nService.i18n("flow.debt.direction.OWE_ME", locale, "\uD83D\uDE03 Вам должны"),
                payload = FlowCallbackPayload(key.value, "DIRECTION:${DebtDirection.OWE_ME.name}"),
                row = 0,
                col = 0
            ),
            FlowInlineButton(
                text = i18nService.i18n("flow.debt.direction.I_OWE", locale, "\uD83D\uDE2B Я должен"),
                payload = FlowCallbackPayload(key.value, "DIRECTION:${DebtDirection.I_OWE.name}"),
                row = 0,
                col = 1
            ),
            FlowInlineButton(
                text = i18nService.i18n("flow.debt.button.edit", locale, "✏\uFE0F Изменить"),
                payload = FlowCallbackPayload(key.value, "ACTION:EDIT_MENU"),
                row = 1,
                col = 0
            ),
            FlowInlineButton(
                text = i18nService.i18n("flow.debt.button.remove", locale, "\uD83D\uDDD1 Удалить"),
                payload = FlowCallbackPayload(key.value, "ACTION:REMOVE_MENU"),
                row = 1,
                col = 1
            )
        )

    private fun buildRemoveButtons(locale: Locale, overview: DebtOverviewModel): List<FlowInlineButton> {
        val debts = (overview.oweMe + overview.iOwe)
        val items = debts.mapIndexed { index, debt ->
            FlowInlineButton(
                text = "❌ ${debt.displayNumber}",
                payload = FlowCallbackPayload(key.value, "REMOVE:${debt.id}"),
                row = index / REMOVE_COLUMNS,
                col = index % REMOVE_COLUMNS
            )
        }
        val back = FlowInlineButton(
            text = i18nService.i18n("flow.button.back", locale, "⬅️ Назад"),
            payload = FlowCallbackPayload(key.value, "ACTION:BACK"),
            row = (items.lastOrNull()?.row ?: 0) + 1,
            col = 0
        )
        return items + back
    }


    private fun buildEditButtons(locale: Locale, overview: DebtOverviewModel): List<FlowInlineButton> {
        val debts = (overview.oweMe + overview.iOwe)
        val items = debts.mapIndexed { index, debt ->
            FlowInlineButton(
                text = "✏\uFE0F ${debt.displayNumber}",
                payload = FlowCallbackPayload(key.value, "EDIT:${debt.id}"),
                row = index / REMOVE_COLUMNS,
                col = index % REMOVE_COLUMNS
            )
        }
        val back = FlowInlineButton(
            text = i18nService.i18n("flow.button.back", locale, "⬅️ Назад"),
            payload = FlowCallbackPayload(key.value, "ACTION:BACK"),
            row = (items.lastOrNull()?.row ?: 0) + 1,
            col = 0
        )
        return items + back
    }

    private fun buildCreateMessage(
        locale: Locale,
        creation: DebtCreationState,
        phase: DebtCreationPhase,
        servers: List<Server>
    ): FlowMessage {
        val model = DebtCreationViewModel(
            phase = phase,
            direction = creation.direction?.let { directionLabel(it, locale) },
            server = creation.serverId?.let { id -> servers.firstOrNull { it.id == id }?.name ?: "#$id" },
            resource = creation.resourceType?.let { resourceLabel(it, locale) },
            amount = creation.amount,
            counterparty = creation.counterpartyName
        )
        val buttons = when (phase) {
            DebtCreationPhase.DIRECTION -> buildDirectionButtons(locale)
            DebtCreationPhase.SERVER -> buildServerButtons(servers)
            DebtCreationPhase.RESOURCE -> buildResourceButtons(locale)
            DebtCreationPhase.AMOUNT, DebtCreationPhase.NAME -> emptyList()
        } + listOf(
            FlowInlineButton(
                text = i18nService.i18n("flow.button.cancel", locale, "❌ Отмена"),
                payload = FlowCallbackPayload(key.value, "ACTION:CANCEL"),
                row = 9,
                col = 0
            )
        )
        return key.buildMessage(
            step = DebtStep.CREATE,
            model = model,
            inlineButtons = buttons
        )
    }

    private fun buildDirectionButtons(locale: Locale): List<FlowInlineButton> = listOf(
        FlowInlineButton(
            text = i18nService.i18n("flow.debt.direction.OWE_ME", locale, "\uD83D\uDE00 Мне должны"),
            payload = FlowCallbackPayload(key.value, "DIRECTION:${DebtDirection.OWE_ME.name}"),
            row = 0,
            col = 0
        ),
        FlowInlineButton(
            text = i18nService.i18n("flow.debt.direction.I_OWE", locale, "\uD83D\uDE2B Я должен"),
            payload = FlowCallbackPayload(key.value, "DIRECTION:${DebtDirection.I_OWE.name}"),
            row = 0,
            col = 1
        )
    )

    private fun buildServerButtons(servers: List<Server>): List<FlowInlineButton> =
        servers.sortedBy { it.id }.mapIndexed { index, server ->
            FlowInlineButton(
                text = server.id.toString(),
                payload = FlowCallbackPayload(key.value, "SERVER:${server.id}"),
                row = index / 5,
                col = index % 5
            )
        }

    private fun buildResourceButtons(locale: Locale): List<FlowInlineButton> =
        DebtResourceType.entries.toTypedArray().mapIndexed { index, type ->
            FlowInlineButton(
                text = resourceLabel(type, locale),
                payload = FlowCallbackPayload(key.value, "RESOURCE:${type.name}"),
                row = index / 5,
                col = index % 5
            )
        }

    private fun buildAmountPrompt(locale: Locale, creation: DebtCreationState, invalid: Boolean = false): FlowMessage =
        key.buildMessage(
            step = DebtStep.PROMPT_AMOUNT,
            model = DebtPromptModel(
                title = i18nService.i18n("flow.debt.prompt.amount", locale, "Введите количество"),
                invalid = invalid,
                creation = creation.view(locale)
            ),
            inlineButtons = listOf(
                key.cancelPromptButton(
                    text = i18nService.i18n("flow.button.cancel", locale, "❌ Отмена")
                )
            )
        )

    private fun buildNamePrompt(locale: Locale, creation: DebtCreationState, invalid: Boolean = false): FlowMessage =
        key.buildMessage(
            step = DebtStep.PROMPT_NAME,
            model = DebtPromptModel(
                title = i18nService.i18n("flow.debt.prompt.name", locale, "Введите имя должника/кредитора"),
                invalid = invalid,
                creation = creation.view(locale)
            ),
            inlineButtons = listOf(
                key.cancelPromptButton(
                    text = i18nService.i18n("flow.button.cancel", locale, "❌ Отмена")
                )
            )
        )

    private fun buildEditAmountPrompt(
        locale: Locale,
        debt: DebtItemModel,
        invalid: Boolean = false
    ): FlowMessage =
        key.buildMessage(
            step = DebtStep.PROMPT_AMOUNT,
            model = DebtPromptModel(
                title = i18nService.i18n("flow.debt.prompt.amount", locale, "Введите количество"),
                invalid = invalid,
                creation = debt.toCreationView()
            ),
            inlineButtons = listOf(
                key.cancelPromptButton(
                    text = i18nService.i18n("flow.button.cancel", locale, "❌ Отмена")
                )
            )
        )

    private fun DebtCreationState.view(locale: Locale): DebtCreationViewModel = DebtCreationViewModel(
        phase = phase,
        direction = direction?.let { directionLabel(it, locale) },
        server = serverId?.toString(),
        resource = resourceType?.let { resourceLabel(it, locale) },
        amount = amount,
        counterparty = counterpartyName
    )

    private fun DebtItemModel.toCreationView(): DebtCreationViewModel = DebtCreationViewModel(
        phase = DebtCreationPhase.AMOUNT,
        direction = directionLabel,
        server = server,
        resource = resourceLabel,
        amount = amount,
        counterparty = counterparty
    )

    companion object {
        private const val MAIN_MESSAGE_KEY = "debt_main"
        private const val PROMPT_BINDING_PREFIX = "debt_prompt"
        private const val EDIT_PROMPT_BINDING_PREFIX = "debt_edit_prompt"
        private const val REMOVE_COLUMNS = 5
    }
}

data class DebtOverviewModel(
    val oweMe: List<DebtItemModel>,
    val iOwe: List<DebtItemModel>,
)

data class DebtItemModel(
    val id: Long,
    val displayNumber: Int,
    val directionLabel: String,
    val resourceLabel: String,
    val amount: Int,
    val server: String,
    val counterparty: String,
)

data class DebtCreationViewModel(
    val phase: DebtCreationPhase,
    val direction: String?,
    val server: String?,
    val resource: String?,
    val amount: Int?,
    val counterparty: String?,
)

data class DebtPromptModel(
    val title: String,
    val invalid: Boolean,
    val creation: DebtCreationViewModel,
)








