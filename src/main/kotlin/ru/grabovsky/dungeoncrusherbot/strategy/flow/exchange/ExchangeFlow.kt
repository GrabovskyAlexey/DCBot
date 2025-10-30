
package ru.grabovsky.dungeoncrusherbot.strategy.flow.exchange

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeDirectionType
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequest
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequestType
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequestType.BUY_MAP
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequestType.EXCHANGE_MAP
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequestType.EXCHANGE_VOID
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequestType.SELL_MAP
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeResourceType
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeResourceType.MAP
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeResourceType.VOID
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ExchangeRequestService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeDetailDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeRequestDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeResultDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.PriceDto
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.AnswerCallbackAction
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
import java.util.Locale
import org.telegram.telegrambots.meta.api.objects.message.Message as TgMessage
import ru.grabovsky.dungeoncrusherbot.entity.User as BotUser

@Component
class ExchangeFlow(
    private val userService: UserService,
    private val serverService: ServerService,
    private val exchangeRequestService: ExchangeRequestService,
    private val i18nService: I18nService,
) : FlowHandler<ExchangeFlowState> {

    override val key: FlowKey = FlowKeys.EXCHANGE
    override val payloadType: Class<ExchangeFlowState> = ExchangeFlowState::class.java

    override fun start(context: FlowStartContext): FlowResult<ExchangeFlowState> {
        val user = getUserEntity(context.user.id)
        val model = buildOverviewModel(user)
        return FlowResult(
            stepKey = ExchangeFlowStep.MAIN.key,
            payload = ExchangeFlowState(),
            actions = listOf(
                SendMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildMainMessage(model, context.locale)
                )
            )
        )
    }

    override fun onMessage(
        context: FlowMessageContext<ExchangeFlowState>,
        message: TgMessage
    ): FlowResult<ExchangeFlowState>? = null

    override fun onCallback(
        context: FlowCallbackContext<ExchangeFlowState>,
        callbackQuery: CallbackQuery,
        data: String
    ): FlowResult<ExchangeFlowState>? {
        val (command, argument) = parseCallback(data)
        return when (command) {
            "SERVER" -> argument?.toIntOrNull()?.let { openDetail(context, callbackQuery, it) }
            "ACTION" -> argument?.let { handleDetailAction(context, callbackQuery, it) }
            "TARGET_SERVER" -> argument?.let { handleTargetServer(context, callbackQuery, it) }
            "SOURCE_PRICE" -> argument?.let { handleSourcePrice(context, callbackQuery, it) }
            "TARGET_PRICE" -> argument?.let { handleTargetPrice(context, callbackQuery, it) }
            "REMOVE" -> argument?.let { handleRemove(context, callbackQuery, it) }
            "SEARCH" -> argument?.let { handleSearch(context, callbackQuery, it) }
            "SEARCH_RESULT" -> argument?.let { handleSearchResult(context, callbackQuery, it) }
            "MAIN" -> showMain(context, callbackQuery)
            else -> null
        }
    }

    private fun showMain(
        context: FlowCallbackContext<ExchangeFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<ExchangeFlowState> {
        val user = getUserEntity(context.user.id)
        val model = buildOverviewModel(user)
        return FlowResult(
            stepKey = ExchangeFlowStep.MAIN.key,
            payload = ExchangeFlowState(),
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildMainMessage(model, context.locale)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun openDetail(
        context: FlowCallbackContext<ExchangeFlowState>,
        callbackQuery: CallbackQuery,
        serverId: Int
    ): FlowResult<ExchangeFlowState> {
        val user = getUserEntity(context.user.id)
        val detail = buildDetailModel(user, serverId)
        val payload = ExchangeFlowState(selectedServerId = serverId)
        return FlowResult(
            stepKey = ExchangeFlowStep.DETAIL.key,
            payload = payload,
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildDetailMessage(detail, context.locale)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun showDetail(
        context: FlowCallbackContext<ExchangeFlowState>,
        callbackQuery: CallbackQuery,
        state: ExchangeFlowState
    ): FlowResult<ExchangeFlowState> {
        val serverId = state.selectedServerId ?: return showMain(context, callbackQuery)
        val user = getUserEntity(context.user.id)
        val detail = buildDetailModel(user, serverId)
        val payload = state.copy(pendingRequest = null)
        return FlowResult(
            stepKey = ExchangeFlowStep.DETAIL.key,
            payload = payload,
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildDetailMessage(detail, context.locale)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }
    private fun handleDetailAction(
        context: FlowCallbackContext<ExchangeFlowState>,
        callbackQuery: CallbackQuery,
        action: String
    ): FlowResult<ExchangeFlowState>? {
        val state = context.state.payload
        return when (action) {
            "BACK" -> showMain(context, callbackQuery)
            "EXCHANGE_MAP" -> enterTargetServer(context, callbackQuery, state, EXCHANGE_MAP)
            "EXCHANGE_VOID" -> enterTargetServer(context, callbackQuery, state, EXCHANGE_VOID)
            "SELL_MAP" -> enterSourcePrice(context, callbackQuery, state, SELL_MAP)
            "BUY_MAP" -> enterSourcePrice(context, callbackQuery, state, BUY_MAP)
            "REMOVE_EXCHANGE_REQUEST" -> enterRemove(context, callbackQuery, state)
            "SEARCH_EXCHANGE" -> enterSearch(context, callbackQuery, state)
            else -> AnswerCallbackAction(callbackQuery.id).asResult(state, context.state.stepKey)
        }
    }

    private fun enterTargetServer(
        context: FlowCallbackContext<ExchangeFlowState>,
        callbackQuery: CallbackQuery,
        state: ExchangeFlowState,
        type: ExchangeRequestType
    ): FlowResult<ExchangeFlowState>? {
        val serverId = state.selectedServerId ?: return showMain(context, callbackQuery)
        val pending = ExchangeFlowState.PendingRequest(
            type = type,
            sourceServerId = serverId,
            sourceResourceType = if (type == EXCHANGE_VOID) VOID else MAP,
            targetResourceType = if (type == EXCHANGE_VOID) VOID else MAP,
            sourcePrice = 1,
            targetPrice = 1,
        )
        val user = getUserEntity(context.user.id)
        val model = buildOverviewModel(user)
        val payload = state.copy(pendingRequest = pending)
        return FlowResult(
            stepKey = ExchangeFlowStep.TARGET_SERVER.key,
            payload = payload,
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildTargetServerMessage(model, context.locale)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun handleTargetServer(
        context: FlowCallbackContext<ExchangeFlowState>,
        callbackQuery: CallbackQuery,
        argument: String
    ): FlowResult<ExchangeFlowState>? {
        val state = context.state.payload
        if (argument == "BACK") {
            return showDetail(context, callbackQuery, state)
        }
        val serverId = argument.toIntOrNull() ?: return AnswerCallbackAction(callbackQuery.id)
            .asResult(state, context.state.stepKey)
        val pending = state.pendingRequest ?: return showDetail(context, callbackQuery, state)
        val updatedPending = pending.copy(targetServerId = serverId)
        return finalizeRequest(context, callbackQuery, state.copy(pendingRequest = updatedPending))
    }

    private fun enterSourcePrice(
        context: FlowCallbackContext<ExchangeFlowState>,
        callbackQuery: CallbackQuery,
        state: ExchangeFlowState,
        type: ExchangeRequestType
    ): FlowResult<ExchangeFlowState>? {
        val serverId = state.selectedServerId ?: return showMain(context, callbackQuery)
        val pending = ExchangeFlowState.PendingRequest(
            type = type,
            sourceServerId = serverId,
            targetServerId = serverId,
            sourceResourceType = if (type == SELL_MAP) MAP else VOID,
            targetResourceType = if (type == SELL_MAP) VOID else MAP,
        )
        val payload = state.copy(pendingRequest = pending)
        val model = PriceDto(resource = resourceLabelForSource(pending.type, context.locale))
        return FlowResult(
            stepKey = ExchangeFlowStep.SOURCE_PRICE.key,
            payload = payload,
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildSourcePriceMessage(model, context.locale)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun handleSourcePrice(
        context: FlowCallbackContext<ExchangeFlowState>,
        callbackQuery: CallbackQuery,
        argument: String
    ): FlowResult<ExchangeFlowState>? {
        val state = context.state.payload
        if (argument == "BACK") {
            return showDetail(context, callbackQuery, state)
        }
        val price = argument.toIntOrNull() ?: return AnswerCallbackAction(callbackQuery.id)
            .asResult(state, context.state.stepKey)
        val pending = state.pendingRequest ?: return showDetail(context, callbackQuery, state)
        val updated = pending.copy(sourcePrice = price)
        val payload = state.copy(pendingRequest = updated)
        val model = PriceDto(resource = resourceLabelForTarget(updated.type, context.locale))
        return FlowResult(
            stepKey = ExchangeFlowStep.TARGET_PRICE.key,
            payload = payload,
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildTargetPriceMessage(model, context.locale)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun handleTargetPrice(
        context: FlowCallbackContext<ExchangeFlowState>,
        callbackQuery: CallbackQuery,
        argument: String
    ): FlowResult<ExchangeFlowState>? {
        val state = context.state.payload
        if (argument == "BACK") {
            return showDetail(context, callbackQuery, state.copy(pendingRequest = null))
        }
        val price = argument.toIntOrNull() ?: return AnswerCallbackAction(callbackQuery.id)
            .asResult(state, context.state.stepKey)
        val pending = state.pendingRequest ?: return showDetail(context, callbackQuery, state)
        val updated = pending.copy(targetPrice = price)
        return finalizeRequest(context, callbackQuery, state.copy(pendingRequest = updated))
    }

    private fun finalizeRequest(
        context: FlowCallbackContext<ExchangeFlowState>,
        callbackQuery: CallbackQuery,
        state: ExchangeFlowState
    ): FlowResult<ExchangeFlowState> {
        val pending = state.pendingRequest
        val serverId = state.selectedServerId
        if (pending == null || serverId == null) {
            logger.warn { "Finalize request without pending data for user ${context.user.id}" }
            return showDetail(context, callbackQuery, state.copy(pendingRequest = null))
        }
        val result = runCatching {
            val user = getUserEntity(context.user.id)
            exchangeRequestService.createOrUpdateExchangeRequest(
                user = user,
                exchangeRequestType = pending.type,
                sourceServerId = pending.sourceServerId,
                targetServerId = pending.targetServerId,
                sourceResourceType = pending.sourceResourceType,
                targetResourceType = pending.targetResourceType,
                sourcePrice = pending.sourcePrice ?: 1,
                targetPrice = pending.targetPrice ?: 1,
            )
        }.onFailure {
            logger.warn(it) { "Failed to create exchange request for user ${context.user.id}" }
        }
        val payload = state.copy(pendingRequest = null)
        return if (result.isFailure) {
            FlowResult(
                stepKey = context.state.stepKey,
                payload = payload,
                actions = listOf(
                    AnswerCallbackAction(
                        callbackQueryId = callbackQuery.id,
                        text = i18nService.i18n(
                            "flow.exchange.error.create",
                            context.locale,
                            "Не удалось создать заявку, попробуй ещё раз."
                        ),
                        showAlert = true
                    )
                )
            )
        } else {
            showDetail(context, callbackQuery, payload)
        }
    }

    private fun enterRemove(
        context: FlowCallbackContext<ExchangeFlowState>,
        callbackQuery: CallbackQuery,
        state: ExchangeFlowState
    ): FlowResult<ExchangeFlowState>? {
        val serverId = state.selectedServerId ?: return showMain(context, callbackQuery)
        val user = getUserEntity(context.user.id)
        val detail = buildDetailModel(user, serverId)
        return FlowResult(
            stepKey = ExchangeFlowStep.REMOVE.key,
            payload = state.copy(pendingRequest = null),
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildRemoveMessage(detail, context.locale)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun handleRemove(
        context: FlowCallbackContext<ExchangeFlowState>,
        callbackQuery: CallbackQuery,
        argument: String
    ): FlowResult<ExchangeFlowState>? {
        val state = context.state.payload
        if (argument == "BACK") {
            return showDetail(context, callbackQuery, state)
        }
        val requestId = argument.toLongOrNull() ?: return AnswerCallbackAction(callbackQuery.id)
            .asResult(state, context.state.stepKey)
        runCatching {
            exchangeRequestService.setRequestInactiveById(requestId)
        }.onFailure {
            logger.warn(it) { "Failed to deactivate request $requestId for user ${context.user.id}" }
            return FlowResult(
                stepKey = context.state.stepKey,
                payload = state,
                actions = listOf(
                    AnswerCallbackAction(
                        callbackQueryId = callbackQuery.id,
                        text = i18nService.i18n(
                            "flow.exchange.error.remove",
                            context.locale,
                            "Не удалось удалить заявку, попробуй ещё раз."
                        ),
                        showAlert = true
                    )
                )
            )
        }
        return showDetail(context, callbackQuery, state)
    }
    private fun enterSearch(
        context: FlowCallbackContext<ExchangeFlowState>,
        callbackQuery: CallbackQuery,
        state: ExchangeFlowState
    ): FlowResult<ExchangeFlowState>? {
        val serverId = state.selectedServerId ?: return showMain(context, callbackQuery)
        val user = getUserEntity(context.user.id)
        val detail = buildSearchModel(user, serverId)
        return FlowResult(
            stepKey = ExchangeFlowStep.SEARCH.key,
            payload = state.copy(pendingRequest = null),
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_KEY,
                    message = buildSearchMessage(detail, context.locale)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun handleSearch(
        context: FlowCallbackContext<ExchangeFlowState>,
        callbackQuery: CallbackQuery,
        argument: String
    ): FlowResult<ExchangeFlowState>? {
        val state = context.state.payload
        if (argument == "BACK") {
            return showDetail(context, callbackQuery, state)
        }
        val requestId = argument.toLongOrNull() ?: return AnswerCallbackAction(callbackQuery.id)
            .asResult(state, context.state.stepKey)
        val result = runCatching { buildSearchResultModel(requestId) }
        return result.fold(
            onSuccess = { model ->
                FlowResult(
                    stepKey = ExchangeFlowStep.SEARCH_RESULT.key,
                    payload = state,
                    actions = listOf(
                        EditMessageAction(
                            bindingKey = MAIN_MESSAGE_KEY,
                            message = buildSearchResultMessage(model, context.locale)
                        ),
                        AnswerCallbackAction(callbackQuery.id)
                    )
                )
            },
            onFailure = {
                logger.warn(it) { "Failed to load search result $requestId for user ${context.user.id}" }
                FlowResult(
                    stepKey = context.state.stepKey,
                    payload = state,
                    actions = listOf(
                        AnswerCallbackAction(
                            callbackQueryId = callbackQuery.id,
                            text = i18nService.i18n(
                                "flow.exchange.error.search",
                                context.locale,
                                "Не удалось загрузить заявку."
                            ),
                            showAlert = true
                        )
                    )
                )
            }
        )
    }

    private fun handleSearchResult(
        context: FlowCallbackContext<ExchangeFlowState>,
        callbackQuery: CallbackQuery,
        argument: String
    ): FlowResult<ExchangeFlowState>? {
        val state = context.state.payload
        return if (argument == "BACK") {
            enterSearch(context, callbackQuery, state)
        } else {
            AnswerCallbackAction(callbackQuery.id).asResult(state, context.state.stepKey)
        }
    }

    private fun buildOverviewModel(user: BotUser): ExchangeDto {
        val mainServerId = user.profile?.mainServerId
        val requestServers = exchangeRequestService.getActiveExchangeRequestsByUser(user)
            .filter { it.isActive }
            .map { it.sourceServerId }
            .toSet()
        val servers = serverService.getAllServers()
            .sortedBy { it.id }
            .map { server ->
                ExchangeDto.Server(
                    id = server.id,
                    main = mainServerId == server.id,
                    hasRequests = requestServers.contains(server.id),
                )
            }
        return ExchangeDto(
            servers = servers,
            username = user.userName
        )
    }

    private fun buildDetailModel(user: BotUser, serverId: Int): ExchangeDetailDto {
        val requests = exchangeRequestService.getActiveExchangeRequestsByServer(
            user,
            serverId,
            ExchangeDirectionType.SOURCE
        )
            .filter { it.isActive }
            .mapIndexed { index, request -> request.toDto(index + 1) }

        return ExchangeDetailDto(
            username = user.userName ?: user.firstName,
            serverId = serverId,
            requests = requests
        )
    }

    private fun buildSearchModel(user: BotUser, serverId: Int): ExchangeDetailDto {
        val requests = exchangeRequestService.getActiveExchangeRequestsByServerExcludeSelfExchange(
            user,
            serverId,
            ExchangeDirectionType.TARGET
        )
            .filter { it.isActive }
            .mapIndexed { index, request -> request.toDto(index + 1) }

        return ExchangeDetailDto(
            username = user.userName ?: user.firstName,
            serverId = serverId,
            requests = requests
        )
    }

    private fun buildSearchResultModel(requestId: Long): ExchangeResultDto {
        val request = exchangeRequestService.getRequestById(requestId)
            ?: throw IllegalStateException("Exchange request $requestId not found")
        return ExchangeResultDto(
            username = escapeMarkdown(request.user.userName),
            firstName = request.user.firstName ?: "",
            active = request.isActive,
            request = request.toDto(pos = 1)
        )
    }

    private fun buildMainMessage(model: ExchangeDto, locale: Locale): FlowMessage = FlowMessage(
        flowKey = key,
        stepKey = ExchangeFlowStep.MAIN.key,
        model = model,
        inlineButtons = buildServerButtons(model)
    )

    private fun buildDetailMessage(detail: ExchangeDetailDto, locale: Locale): FlowMessage = FlowMessage(
        flowKey = key,
        stepKey = ExchangeFlowStep.DETAIL.key,
        model = detail,
        inlineButtons = buildDetailButtons(locale, detail)
    )

    private fun buildTargetServerMessage(model: ExchangeDto, locale: Locale): FlowMessage = FlowMessage(
        flowKey = key,
        stepKey = ExchangeFlowStep.TARGET_SERVER.key,
        model = model,
        inlineButtons = buildTargetServerButtons(model, locale)
    )

    private fun buildSourcePriceMessage(model: PriceDto, locale: Locale): FlowMessage = FlowMessage(
        flowKey = key,
        stepKey = ExchangeFlowStep.SOURCE_PRICE.key,
        model = model,
        inlineButtons = buildNumericButtons("SOURCE_PRICE")
    )

    private fun buildTargetPriceMessage(model: PriceDto, locale: Locale): FlowMessage = FlowMessage(
        flowKey = key,
        stepKey = ExchangeFlowStep.TARGET_PRICE.key,
        model = model,
        inlineButtons = buildNumericButtons("TARGET_PRICE")
    )

    private fun buildRemoveMessage(detail: ExchangeDetailDto, locale: Locale): FlowMessage = FlowMessage(
        flowKey = key,
        stepKey = ExchangeFlowStep.REMOVE.key,
        model = detail,
        inlineButtons = buildRemoveButtons(detail, locale)
    )

    private fun buildSearchMessage(detail: ExchangeDetailDto, locale: Locale): FlowMessage = FlowMessage(
        flowKey = key,
        stepKey = ExchangeFlowStep.SEARCH.key,
        model = detail,
        inlineButtons = buildSearchButtons(detail, locale)
    )

    private fun buildSearchResultMessage(result: ExchangeResultDto, locale: Locale): FlowMessage = FlowMessage(
        flowKey = key,
        stepKey = ExchangeFlowStep.SEARCH_RESULT.key,
        model = result,
        inlineButtons = listOf(
            FlowInlineButton(
                text = i18nService.i18n("buttons.exchange.back", locale, "↩ Назад"),
                payload = FlowCallbackPayload(key.value, "SEARCH_RESULT:BACK"),
                row = 0,
                col = 0
            )
        )
    )
    private fun buildServerButtons(model: ExchangeDto): List<FlowInlineButton> {
        var row = 0
        var col = 0
        val buttons = mutableListOf<FlowInlineButton>()
        model.servers.forEach { server ->
            buttons += FlowInlineButton(
                text = buildServerLabel(server),
                payload = FlowCallbackPayload(key.value, "SERVER:${server.id}"),
                row = row,
                col = col
            )
            col++
            if (col >= 5) {
                col = 0
                row++
            }
        }
        return buttons
    }

    private fun buildDetailButtons(locale: Locale, detail: ExchangeDetailDto): List<FlowInlineButton> {
        val buttons = mutableListOf(
            FlowInlineButton(
                text = i18nService.i18n("buttons.exchange.request.create.exchange_map", locale, "🪆 Обмен карт"),
                payload = FlowCallbackPayload(key.value, "ACTION:EXCHANGE_MAP"),
                row = 0,
                col = 0
            ),
            FlowInlineButton(
                text = i18nService.i18n("buttons.exchange.request.create.exchange_void", locale, "🟣 Обмен пустот"),
                payload = FlowCallbackPayload(key.value, "ACTION:EXCHANGE_VOID"),
                row = 0,
                col = 1
            ),
            FlowInlineButton(
                text = i18nService.i18n("buttons.exchange.request.create.sell", locale, "🪆 Продать"),
                payload = FlowCallbackPayload(key.value, "ACTION:SELL_MAP"),
                row = 1,
                col = 0
            ),
            FlowInlineButton(
                text = i18nService.i18n("buttons.exchange.request.create.buy", locale, "🪆 Купить"),
                payload = FlowCallbackPayload(key.value, "ACTION:BUY_MAP"),
                row = 1,
                col = 1
            ),
            FlowInlineButton(
                text = i18nService.i18n("buttons.exchange.search", locale, "🔍 Поиск"),
                payload = FlowCallbackPayload(key.value, "ACTION:SEARCH_EXCHANGE"),
                row = 2,
                col = 0
            ),
            FlowInlineButton(
                text = i18nService.i18n("buttons.exchange.back", locale, "↩ Назад"),
                payload = FlowCallbackPayload(key.value, "ACTION:BACK"),
                row = 4,
                col = 0
            )
        )
        if (detail.requests.isNotEmpty()) {
            buttons += FlowInlineButton(
                text = i18nService.i18n("buttons.exchange.remove", locale, "✖ Удалить"),
                payload = FlowCallbackPayload(key.value, "ACTION:REMOVE_EXCHANGE_REQUEST"),
                row = 3,
                col = 0
            )
        }
        return buttons
    }

    private fun buildTargetServerButtons(model: ExchangeDto, locale: Locale): List<FlowInlineButton> {
        val buttons = mutableListOf<FlowInlineButton>()
        var row = 0
        var col = 0
        model.servers.forEach { server ->
            buttons += FlowInlineButton(
                text = buildServerLabel(server),
                payload = FlowCallbackPayload(key.value, "TARGET_SERVER:${server.id}"),
                row = row,
                col = col
            )
            col++
            if (col >= 5) {
                col = 0
                row++
            }
        }
        buttons += FlowInlineButton(
            text = i18nService.i18n("buttons.exchange.back", locale, "↩ Назад"),
            payload = FlowCallbackPayload(key.value, "TARGET_SERVER:BACK"),
            row = row + 1,
            col = 0
        )
        return buttons
    }

    private fun buildNumericButtons(command: String): List<FlowInlineButton> {
        val buttons = mutableListOf<FlowInlineButton>()
        var row = 0
        var col = 0
        for (value in 1..10) {
            buttons += FlowInlineButton(
                text = value.toString(),
                payload = FlowCallbackPayload(key.value, "$command:$value"),
                row = row,
                col = col
            )
            col++
            if (col >= 5) {
                col = 0
                row++
            }
        }
        buttons += FlowInlineButton(
            text = "↩",
            payload = FlowCallbackPayload(key.value, "$command:BACK"),
            row = row + 1,
            col = 0
        )
        return buttons
    }

    private fun buildRemoveButtons(detail: ExchangeDetailDto, locale: Locale): List<FlowInlineButton> {
        val buttons = mutableListOf<FlowInlineButton>()
        var row = 0
        var col = 0
        detail.requests.forEach { request ->
            buttons += FlowInlineButton(
                text = "№${request.pos}",
                payload = FlowCallbackPayload(key.value, "REMOVE:${request.id}"),
                row = row,
                col = col
            )
            col++
            if (col >= 5) {
                col = 0
                row++
            }
        }
        buttons += FlowInlineButton(
            text = i18nService.i18n("buttons.exchange.back", locale, "↩ Назад"),
            payload = FlowCallbackPayload(key.value, "REMOVE:BACK"),
            row = row + 1,
            col = 0
        )
        return buttons
    }

    private fun buildSearchButtons(detail: ExchangeDetailDto, locale: Locale): List<FlowInlineButton> {
        val buttons = mutableListOf<FlowInlineButton>()
        var row = 0
        var col = 0
        detail.requests.forEach { request ->
            buttons += FlowInlineButton(
                text = request.pos.toString(),
                payload = FlowCallbackPayload(key.value, "SEARCH:${request.id}"),
                row = row,
                col = col
            )
            col++
            if (col >= 5) {
                col = 0
                row++
            }
        }
        buttons += FlowInlineButton(
            text = i18nService.i18n("buttons.exchange.back", locale, "↩ Назад"),
            payload = FlowCallbackPayload(key.value, "SEARCH:BACK"),
            row = row + 1,
            col = 0
        )
        return buttons
    }

    private fun buildServerLabel(server: ExchangeDto.Server): String {
        val suffix = if (server.main) "👑" else ""
        val prefix = if (server.hasRequests) "✅" else ""
        return "$prefix${server.id}$suffix"
    }

    private fun ExchangeRequest.toDto(pos: Int) = ExchangeRequestDto(
        pos = pos,
        id = this.id!!,
        type = this.type,
        sourcePrice = this.sourceResourcePrice,
        targetPrice = this.targetResourcePrice,
        targetServerId = this.targetServerId,
        sourceServerId = this.sourceServerId
    )

    private fun resourceLabelForSource(
        type: ExchangeRequestType,
        locale: Locale
    ): String = when (type) {
        SELL_MAP -> if (locale.language == "ru") "карт" else "maps"
        BUY_MAP -> if (locale.language == "ru") "пустот" else "voids"
        else -> ""
    }

    private fun resourceLabelForTarget(
        type: ExchangeRequestType,
        locale: Locale
    ): String = when (type) {
        SELL_MAP -> if (locale.language == "ru") "пустот" else "voids"
        BUY_MAP -> if (locale.language == "ru") "карт" else "maps"
        else -> ""
    }

    private fun parseCallback(data: String): Pair<String, String?> =
        if (data.contains(':')) {
            val split = data.split(':', limit = 2)
            split[0] to split[1]
        } else {
            data to null
        }

    private fun getUserEntity(id: Long): BotUser =
        userService.getUser(id) ?: error("User $id not found")

    private fun escapeMarkdown(source: String?): String? {
        source ?: return null
        val regex = Regex("""([_*\\[\\]()~`>#+\-=|{}!\\\\])""")
        return source.replace(regex, """\\\$1""")
    }

    private fun FlowAction.asResult(
        state: ExchangeFlowState,
        stepKey: String
    ): FlowResult<ExchangeFlowState> = FlowResult(
        stepKey = stepKey,
        payload = state,
        actions = listOf(this)
    )

    companion object {
        private const val MAIN_MESSAGE_KEY = "exchange_main_message"
        private val logger = KotlinLogging.logger {}
    }
}
