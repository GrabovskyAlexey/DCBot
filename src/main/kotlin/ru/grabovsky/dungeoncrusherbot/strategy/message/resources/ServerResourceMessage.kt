package ru.grabovsky.dungeoncrusherbot.strategy.message.resources

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerResourceDto
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.*
import java.util.Locale

@Component
class ServerResourceMessage(messageGenerateService: MessageGenerateService) :
    AbstractSendMessage<ServerResourceDto>(messageGenerateService) {
    override fun inlineButtons(user: User, data: ServerResourceDto?, locale: Locale): List<InlineMarkupDataDto> {
        val dto = data ?: return emptyList()
        val buttons = mutableListOf<InlineMarkupDataDto>()

        fun add(row: Int, code: String, default: String, state: StateCode, payload: String = "", vararg args: Any?) {
            buttons += InlineMarkupDataDto(
                rowPos = row,
                text = i18n(code, locale, default, *args),
                data = CallbackObject(state, payload)
            )
        }

        add(2, "buttons.resources.draador.catch", "\uD83E\uDE86 Поймать", SERVER_RESOURCE, "ADD_DRAADOR")
        if (dto.quickResourceEnabled) add(2, "buttons.resources.increment", "+1", INCREMENT_DRAADOR)
        add(2, "buttons.resources.draador.sell", "\uD83E\uDE86 Продать", SERVER_RESOURCE, "SELL_DRAADOR")
        if (dto.quickResourceEnabled) add(2, "buttons.resources.decrement", "-1", DECREMENT_DRAADOR)

        if (!dto.main) {
            add(3, "buttons.resources.draador.receive", "\uD83E\uDE86 Получить", SERVER_RESOURCE, "RECEIVE_DRAADOR")
            if (dto.quickResourceEnabled) add(3, "buttons.resources.increment", "+1", QUICK_RECEIVE_DRAADOR)
            add(3, "buttons.resources.draador.send", "\uD83E\uDE86 Передать", SERVER_RESOURCE, "SEND_DRAADOR")
            if (dto.quickResourceEnabled) add(3, "buttons.resources.decrement", "-1", QUICK_SEND_DRAADOR)
        }

        add(4, "buttons.resources.void.add", "\uD83D\uDFE3 Добавить", SERVER_RESOURCE, "ADD_VOID")
        if (dto.quickResourceEnabled) add(4, "buttons.resources.increment", "+1", INCREMENT_VOID)
        add(4, "buttons.resources.void.remove", "\uD83D\uDFE3 Удалить", SERVER_RESOURCE, "REMOVE_VOID")
        if (dto.quickResourceEnabled) add(4, "buttons.resources.decrement", "-1", DECREMENT_VOID)

        if (dto.cbEnabled) {
            add(5, "buttons.resources.cb.add", "\uD83D\uDE08 Добавить", SERVER_RESOURCE, "ADD_CB")
            if (dto.quickResourceEnabled) add(5, "buttons.resources.increment", "+1", INCREMENT_CB)
            add(5, "buttons.resources.cb.remove", "\uD83D\uDE08 Удалить", SERVER_RESOURCE, "REMOVE_CB")
            if (dto.quickResourceEnabled) add(5, "buttons.resources.decrement", "-1", DECREMENT_CB)
        }

        if (dto.exchange != null && !dto.main) {
            add(1, "buttons.resources.exchange.remove", "\uD83D\uDCB1 Удалить обменник", SERVER_RESOURCE, "REMOVE_EXCHANGE")
        }
        if (!dto.main) {
            val exchangeCode = if (dto.exchange != null) "buttons.resources.exchange.change" else "buttons.resources.exchange.set"
            val exchangeDefault = if (dto.exchange != null) "\uD83D\uDCB1 Изменить обменник" else "\uD83D\uDCB1 Указать обменник"
            add(1, exchangeCode, exchangeDefault, SERVER_RESOURCE, "ADD_EXCHANGE")
        }

        if (dto.main) {
            add(6, "buttons.notes.add", "✍\uFE0F Добавить заметку", SERVER_RESOURCE, "ADD_NOTE")
            if (dto.notes.isNotEmpty()) {
                add(6, "buttons.notes.remove", "❌ Удалить заметку", SERVER_RESOURCE, "REMOVE_NOTE")
            }
            add(7, "buttons.resources.remove_main", "\uD83D\uDEAB Отменить назначение основным", SERVER_RESOURCE, "REMOVE_MAIN")
        } else if (!dto.hasMain) {
            add(6, "buttons.resources.set_main", "\uD83D\uDC51 Сделать основным", SERVER_RESOURCE, "SET_MAIN")
        }

        if (dto.hasHistory) {
            add(98, "buttons.resources.history", "\uD83D\uDDD2 Последние 20 операций", SERVER_RESOURCE, "RESOURCE_HISTORY")
        }

        buttons += InlineMarkupDataDto(
            rowPos = 97,
            text = i18n(
                code = if (dto.notifyDisable) "buttons.resources.notify.resume" else "buttons.resources.notify.stop",
                locale = locale,
                default = if (dto.notifyDisable) "\u274C Продолжить ловлю" else "\u2705 Закончил ловить"
            ),
            data = CallbackObject(SERVER_RESOURCE, "DISABLE_NOTIFY"),
        )
        buttons += InlineMarkupDataDto(
            rowPos = 99,
            text = i18n("buttons.notes.back", locale, "\uD83D\uDD19 Вернуться"),
            data = CallbackObject(SERVER_RESOURCE, "BACK"),
        )

        return buttons
    }
}
