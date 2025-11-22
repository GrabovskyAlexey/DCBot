package ru.grabovsky.dungeoncrusherbot.strategy.flow.debt

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.entity.Debt
import ru.grabovsky.dungeoncrusherbot.entity.DebtDirection
import ru.grabovsky.dungeoncrusherbot.entity.DebtResourceType
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.service.interfaces.DebtService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowCallbackPayload
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowInlineButton
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowMessage
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.buildMessage
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cancelPromptButton
import java.util.Locale

@Component
class DebtViewService(
    private val debtService: DebtService,
    private val serverService: ServerService,
    private val i18nService: I18nService,
) {

    fun buildOverview(userId: Long, locale: Locale): DebtOverviewModel {
        val debts = debtService.getDebts(userId)

        val oweMeOrdered = debts
            .filter { it.direction == DebtDirection.OWE_ME }
            .mapIndexed { index, debt -> debt to (index + 1) }
            .toMap()

        val iOweOrdered = debts
            .filter { it.direction == DebtDirection.I_OWE }
            .mapIndexed { index, debt -> debt to (oweMeOrdered.size + index + 1) }
            .toMap()

        val oweMe = oweMeOrdered.map { (debt, number) ->
            debt.toView(locale, number)
        }
        val iOwe = iOweOrdered.map { (debt, number) ->
            debt.toView(locale, number)
        }

        return DebtOverviewModel(
            oweMe = oweMe,
            iOwe = iOwe
        )
    }

    fun buildMainMessage(locale: Locale, overview: DebtOverviewModel): FlowMessage =
        FlowKeys.DEBT.buildMessage(
            step = DebtStep.MAIN,
            model = overview,
            inlineButtons = buildMainButtons(locale)
        )

    fun buildItemButtons(locale: Locale, overview: DebtOverviewModel, prefix: String, action: String): List<FlowInlineButton> =
        buildActionButtons(
            locale = locale,
            overview = overview,
            textBuilder = { debt -> "$prefix ${debt.displayNumber}" },
            action = action
        )

    fun buildCreateMessage(
        locale: Locale,
        creation: DebtCreationState,
        phase: DebtCreationPhase,
        servers: List<Server> = serverService.getAllServers()
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
                payload = FlowCallbackPayload(FlowKeys.DEBT.value, "ACTION:CANCEL"),
                row = 9,
                col = 0
            )
        )
        return FlowKeys.DEBT.buildMessage(
            step = DebtStep.CREATE,
            model = model,
            inlineButtons = buttons
        )
    }

    fun buildAmountPrompt(
        locale: Locale,
        creation: DebtCreationViewModel,
        invalid: Boolean = false
    ): FlowMessage =
        FlowKeys.DEBT.buildMessage(
            step = DebtStep.PROMPT_AMOUNT,
            model = DebtPromptModel(
                title = i18nService.i18n("flow.debt.prompt.amount", locale, "Введите количество"),
                invalid = invalid,
                creation = creation
            ),
            inlineButtons = listOf(
                FlowKeys.DEBT.cancelPromptButton(
                    text = i18nService.i18n("flow.button.cancel", locale, "❌ Отмена")
                )
            )
        )

    fun buildNamePrompt(locale: Locale, creation: DebtCreationState, invalid: Boolean = false): FlowMessage =
        FlowKeys.DEBT.buildMessage(
            step = DebtStep.PROMPT_NAME,
            model = DebtPromptModel(
                title = i18nService.i18n("flow.debt.prompt.name", locale, "Введите имя должника/кредитора"),
                invalid = invalid,
                creation = creation.view(locale)
            ),
            inlineButtons = listOf(
                FlowKeys.DEBT.cancelPromptButton(
                    text = i18nService.i18n("flow.button.cancel", locale, "❌ Отмена")
                )
            )
        )

    fun DebtCreationState.view(locale: Locale): DebtCreationViewModel = DebtCreationViewModel(
        phase = phase,
        direction = direction?.let { directionLabel(it, locale) },
        server = serverId?.toString(),
        resource = resourceType?.let { resourceLabel(it, locale) },
        amount = amount,
        counterparty = counterpartyName
    )

    fun DebtItemModel.toCreationView(): DebtCreationViewModel = DebtCreationViewModel(
        phase = DebtCreationPhase.AMOUNT,
        direction = directionLabel,
        server = server,
        resource = resourceLabel,
        amount = amount,
        counterparty = counterparty
    )

    private fun Debt.toView(locale: Locale, displayNumber: Int): DebtItemModel =
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

    private fun buildMainButtons(locale: Locale): List<FlowInlineButton> =
        listOf(
            FlowInlineButton(
                text = i18nService.i18n("flow.debt.direction.OWE_ME", locale, "\uD83D\uDE03 Вам должны"),
                payload = FlowCallbackPayload(FlowKeys.DEBT.value, "DIRECTION:${DebtDirection.OWE_ME.name}"),
                row = 0,
                col = 0
            ),
            FlowInlineButton(
                text = i18nService.i18n("flow.debt.direction.I_OWE", locale, "\uD83D\uDE2B Я должен"),
                payload = FlowCallbackPayload(FlowKeys.DEBT.value, "DIRECTION:${DebtDirection.I_OWE.name}"),
                row = 0,
                col = 1
            ),
            FlowInlineButton(
                text = i18nService.i18n("flow.debt.button.edit", locale, "✏️ Изменить"),
                payload = FlowCallbackPayload(FlowKeys.DEBT.value, "ACTION:EDIT_MENU"),
                row = 1,
                col = 0
            ),
            FlowInlineButton(
                text = i18nService.i18n("flow.debt.button.remove", locale, "\uD83D\uDDD1 Удалить"),
                payload = FlowCallbackPayload(FlowKeys.DEBT.value, "ACTION:REMOVE_MENU"),
                row = 1,
                col = 1
            )
        )

    private fun buildActionButtons(
        locale: Locale,
        overview: DebtOverviewModel,
        textBuilder: (DebtItemModel) -> String,
        action: String
    ): List<FlowInlineButton> {
        val debts = (overview.oweMe + overview.iOwe)
        val items = debts.mapIndexed { index, debt ->
            FlowInlineButton(
                text = textBuilder(debt),
                payload = FlowCallbackPayload(FlowKeys.DEBT.value, "$action:${debt.id}"),
                row = index / REMOVE_COLUMNS,
                col = index % REMOVE_COLUMNS
            )
        }
        val back = FlowInlineButton(
            text = i18nService.i18n("flow.button.back", locale, "⬅️ Назад"),
            payload = FlowCallbackPayload(FlowKeys.DEBT.value, "ACTION:BACK"),
            row = (items.lastOrNull()?.row ?: 0) + 1,
            col = 0
        )
        return items + back
    }

    private fun buildDirectionButtons(locale: Locale): List<FlowInlineButton> = listOf(
        FlowInlineButton(
            text = i18nService.i18n("flow.debt.direction.OWE_ME", locale, "\uD83D\uDE00 Мне должны"),
            payload = FlowCallbackPayload(FlowKeys.DEBT.value, "DIRECTION:${DebtDirection.OWE_ME.name}"),
            row = 0,
            col = 0
        ),
        FlowInlineButton(
            text = i18nService.i18n("flow.debt.direction.I_OWE", locale, "\uD83D\uDE2B Я должен"),
            payload = FlowCallbackPayload(FlowKeys.DEBT.value, "DIRECTION:${DebtDirection.I_OWE.name}"),
            row = 0,
            col = 1
        )
    )

    private fun buildServerButtons(servers: List<Server>): List<FlowInlineButton> =
        servers.sortedBy { it.id }.mapIndexed { index, server ->
            FlowInlineButton(
                text = server.id.toString(),
                payload = FlowCallbackPayload(FlowKeys.DEBT.value, "SERVER:${server.id}"),
                row = index / 5,
                col = index % 5
            )
        }

    private fun buildResourceButtons(locale: Locale): List<FlowInlineButton> =
        DebtResourceType.entries.toTypedArray().mapIndexed { index, type ->
            FlowInlineButton(
                text = resourceLabel(type, locale),
                payload = FlowCallbackPayload(FlowKeys.DEBT.value, "RESOURCE:${type.name}"),
                row = index / 5,
                col = index % 5
            )
        }

    companion object {
        private const val REMOVE_COLUMNS = 5
    }
}
