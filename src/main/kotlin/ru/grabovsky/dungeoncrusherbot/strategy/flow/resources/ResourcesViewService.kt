package ru.grabovsky.dungeoncrusherbot.strategy.flow.resources

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.entity.Resources
import ru.grabovsky.dungeoncrusherbot.entity.ServerResourceData
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerResourceDto
import java.util.*
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Component
class ResourcesViewService(
    private val userService: UserService,
    private val serverService: ServerService,
    private val i18nService: I18nService,
) {

    fun ensureResources(user: TgUser) {
        val entity = userService.getUser(user.id) ?: return
        if (entity.resources == null) {
            entity.resources = Resources(user = entity)
            userService.saveUser(entity)
        }
    }

    fun buildOverview(user: TgUser, locale: Locale): ResourcesOverviewModel {
        val entity = userService.getUser(user.id)
            ?: return ResourcesOverviewModel(emptyList(), emptyList())
        val profile = entity.profile ?: return ResourcesOverviewModel(emptyList(), emptyList())
        val resources = entity.resources ?: return ResourcesOverviewModel(emptyList(), emptyList())

        val settings = profile.settings
        val cbEnabled = settings.resourcesCb
        val mainServerId = profile.mainServerId

        val summaries = resources.data.servers
            .filter { entry ->
                entry.value.hasData(cbEnabled) || entry.key == mainServerId
            }
            .map { (serverId, value) ->
                OverviewSummary(
                    id = serverId,
                    statusIcon = if (value.notifyDisable) "\u2705" else "\u274C",
                    main = serverId == mainServerId,
                    exchange = value.exchange,
                    draadorCount = value.draadorCount,
                    balanceLabel = when {
                        value.balance > 0 -> " (+${value.balance})"
                        value.balance < 0 -> " (${value.balance})"
                        else -> ""
                    },
                    voidCount = value.voidCount,
                    cbEnabled = cbEnabled,
                    cbCount = value.cbCount,
                )
            }
            .sortedBy { it.id }

        val buttons = serverService.getAllServers()
            .sortedBy { it.id }
            .mapIndexed { index, server ->
                Button(
                    action = server.id.toString(),
                    label = buttonLabel(server.id, server.id == mainServerId, locale),
                    row = index / 5,
                    col = index % 5
                )
            }

        return ResourcesOverviewModel(summaries, buttons)
    }

    fun buildServer(user: TgUser, serverId: Int, includeHistory: Boolean, locale: Locale): ServerDetail {
        val entity = userService.getUser(user.id)
            ?: throw IllegalStateException("User not found: ${user.id}")
        val profile = entity.profile ?: throw IllegalStateException("Profile not initialized for user: ${user.id}")
        val resources = entity.resources ?: throw IllegalStateException("Resources not initialized")

        val serverData = resources.data.servers.computeIfAbsent(serverId) { ServerResourceData() }
        val history = if (includeHistory) {
            resources.history[serverId]?.map { it.toString() } ?: emptyList()
        } else {
            emptyList()
        }

        val userNotes = profile.notes.toList()

        val dto = ServerResourceDto(
            id = serverId,
            draadorCount = serverData.draadorCount,
            voidCount = serverData.voidCount,
            balance = serverData.balance,
            exchange = serverData.exchange,
            history = if (includeHistory) history else null,
            hasHistory = resources.history[serverId]?.isNotEmpty() == true,
            notifyDisable = serverData.notifyDisable,
            main = serverId == profile.mainServerId,
            cbEnabled = profile.settings.resourcesCb,
            quickResourceEnabled = profile.settings.resourcesQuickChange,
            cbCount = serverData.cbCount,
            notes = if (serverId == profile.mainServerId) userNotes else emptyList(),
            hasMain = profile.mainServerId != null,
        )

        val buttons = buildServerButtons(dto, locale)

        return ServerDetail(dto = dto, history = history, buttons = buttons)
    }

    private fun buildServerButtons(dto: ServerResourceDto, locale: Locale): List<Button> {
        val buttons = mutableListOf<Button>()

        fun label(code: String, default: String): String = i18nService.i18n(code, locale, default)

        fun button(row: Int, col: Int, code: String, default: String, action: String): Button =
            Button(
                label = label(code, default),
                action = action,
                row = row,
                col = col,
            )

        // Draador operations
        buttons += button(2, 1, "buttons.resources.draador.catch", "\uD83E\uDE86 Поймать", "PROMPT_ADD_DRAADOR")
        buttons += button(2, 3, "buttons.resources.draador.sell", "\uD83E\uDE86 Продать", "PROMPT_SELL_DRAADOR")
        // Void operations
        buttons += button(4, 1, "buttons.resources.void.add", "\uD83D\uDFE3 Добавить", "PROMPT_ADD_VOID")
        buttons += button(4, 3, "buttons.resources.void.remove", "\uD83D\uDFE3 Удалить", "PROMPT_REMOVE_VOID")

        if (!dto.main) {
            buttons += button(3, 1, "buttons.resources.draador.receive", "\uD83E\uDE86 Получить", "PROMPT_RECEIVE_DRAADOR")
            buttons += button(3, 3, "buttons.resources.draador.send", "\uD83E\uDE86 Передать", "PROMPT_SEND_DRAADOR")
            if (dto.exchange != null) {
                buttons += button(1, 1, "buttons.resources.exchange.remove", "\uD83D\uDCB1 Удалить обменник", "REMOVE_EXCHANGE")
            }
            val exchangeCode = if (dto.exchange != null) "buttons.resources.exchange.change" else "buttons.resources.exchange.set"
            val exchangeDefault = if (dto.exchange != null) "\uD83D\uDCB1 Изменить обменник" else "\uD83D\uDCB1 Указать обменник"
            buttons += button(1, 2, exchangeCode, exchangeDefault, "PROMPT_ADD_EXCHANGE")
        } else {
            buttons += button(6, 1, "buttons.notes.add", "\u270d\uFE0F Добавить заметку", "PROMPT_ADD_NOTE")
            if (dto.notes.isNotEmpty()) {
                buttons += button(6, 2, "buttons.notes.remove", "\u274c Удалить заметку", "PROMPT_REMOVE_NOTE")
            }
            buttons += button(7, 1, "buttons.resources.remove_main", "\uD83D\uDEAB Отменить назначение основным", "REMOVE_MAIN")
        }



        if (!dto.hasMain) {
            buttons += button(6, 3, "buttons.resources.set_main", "\uD83D\uDC51 Сделать основным", "SET_MAIN")
        }

        if (dto.hasHistory) {
            buttons += button(98, 1, "buttons.resources.history", "\uD83D\uDDD2 Последние 20 операций", "SHOW_HISTORY")
        }

        if (dto.cbEnabled) {
            buttons += button(5, 1, "buttons.resources.cb.add", "\uD83D\uDE08 Добавить", "PROMPT_ADD_CB")
            buttons += button(5, 3, "buttons.resources.cb.remove", "\uD83D\uDE08 Удалить", "PROMPT_REMOVE_CB")
        }

        if (dto.quickResourceEnabled) {
            buttons += button(2, 2, "buttons.resources.increment", "+1", "QUICK_INCREMENT_DRAADOR")
            buttons += button(2, 4, "buttons.resources.decrement", "-1", "QUICK_DECREMENT_DRAADOR")
            buttons += button(4, 2, "buttons.resources.increment", "+1", "QUICK_INCREMENT_VOID")
            buttons += button(4, 4, "buttons.resources.decrement", "-1", "QUICK_DECREMENT_VOID")
            if (!dto.main) {
                buttons += button(3, 2, "buttons.resources.increment", "+1", "QUICK_RECEIVE_DRAADOR")
                buttons += button(3, 4, "buttons.resources.decrement", "-1", "QUICK_SEND_DRAADOR")
            }
            if (dto.cbEnabled) {
                buttons += button(5, 2, "buttons.resources.increment", "+1", "QUICK_INCREMENT_CB")
                buttons += button(5, 4, "buttons.resources.decrement", "-1", "QUICK_DECREMENT_CB")
            }
        }



        val notifyCode = if (dto.notifyDisable) "buttons.resources.notify.resume" else "buttons.resources.notify.stop"
        val notifyDefault = if (dto.notifyDisable) "\u274C Продолжить ловлю" else "\u2705 Закончил ловить"
        buttons += button(97, 1, notifyCode, notifyDefault, "TOGGLE_NOTIFY")

        buttons += button(99, 1, "buttons.notes.back", "\uD83D\uDD19 Вернуться", "BACK")

        return buttons
    }

    private fun buttonLabel(serverId: Int, isMain: Boolean, locale: Locale): String {
        val code = if (isMain) "buttons.resources.server.main" else "buttons.resources.server.regular"
        val default = if (isMain) "\uD83D\uDC51{0}" else "{0}"
        return i18nService.i18n(code, locale, default, serverId)
    }
}
