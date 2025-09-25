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

        return buildList {
            addAll(draadorButtons(dto, locale))
            addAll(voidButtons(dto, locale))
            addAll(cbButtons(dto, locale))
            addAll(exchangeButtons(dto, locale))
            addAll(noteButtons(dto, locale))
            addAll(historyButtons(dto, locale))
            addAll(notifyButtons(dto, locale))
            add(backButton(locale))
        }
    }

    private fun draadorButtons(dto: ServerResourceDto, locale: Locale): List<InlineMarkupDataDto> = buildList {
        add(button(2, locale, "buttons.resources.draador.catch", "🦆 Поймать", SERVER_RESOURCE, "ADD_DRAADOR"))
        if (dto.quickResourceEnabled) {
            add(button(2, locale, "buttons.resources.increment", "+1", INCREMENT_DRAADOR))
        }
        add(button(2, locale, "buttons.resources.draador.sell", "🦆 Продать", SERVER_RESOURCE, "SELL_DRAADOR"))
        if (dto.quickResourceEnabled) {
            add(button(2, locale, "buttons.resources.decrement", "-1", DECREMENT_DRAADOR))
        }
        if (!dto.main) {
            add(button(3, locale, "buttons.resources.draador.receive", "🦆 Получить", SERVER_RESOURCE, "RECEIVE_DRAADOR"))
            if (dto.quickResourceEnabled) {
                add(button(3, locale, "buttons.resources.increment", "+1", QUICK_RECEIVE_DRAADOR))
            }
            add(button(3, locale, "buttons.resources.draador.send", "🦆 Передать", SERVER_RESOURCE, "SEND_DRAADOR"))
            if (dto.quickResourceEnabled) {
                add(button(3, locale, "buttons.resources.decrement", "-1", QUICK_SEND_DRAADOR))
            }
        }
    }

    private fun voidButtons(dto: ServerResourceDto, locale: Locale): List<InlineMarkupDataDto> = buildList {
        add(button(4, locale, "buttons.resources.void.add", "🟣 Добавить", SERVER_RESOURCE, "ADD_VOID"))
        if (dto.quickResourceEnabled) {
            add(button(4, locale, "buttons.resources.increment", "+1", INCREMENT_VOID))
        }
        add(button(4, locale, "buttons.resources.void.remove", "🟣 Удалить", SERVER_RESOURCE, "REMOVE_VOID"))
        if (dto.quickResourceEnabled) {
            add(button(4, locale, "buttons.resources.decrement", "-1", DECREMENT_VOID))
        }
    }

    private fun cbButtons(dto: ServerResourceDto, locale: Locale): List<InlineMarkupDataDto> {
        if (!dto.cbEnabled) return emptyList()

        return buildList {
            add(button(5, locale, "buttons.resources.cb.add", "😈 Добавить", SERVER_RESOURCE, "ADD_CB"))
            if (dto.quickResourceEnabled) {
                add(button(5, locale, "buttons.resources.increment", "+1", INCREMENT_CB))
            }
            add(button(5, locale, "buttons.resources.cb.remove", "😈 Удалить", SERVER_RESOURCE, "REMOVE_CB"))
            if (dto.quickResourceEnabled) {
                add(button(5, locale, "buttons.resources.decrement", "-1", DECREMENT_CB))
            }
        }
    }

    private fun exchangeButtons(dto: ServerResourceDto, locale: Locale): List<InlineMarkupDataDto> {
        if (dto.main) return emptyList()

        return buildList {
            if (dto.exchange != null) {
                add(button(1, locale, "buttons.resources.exchange.remove", "💱 Удалить обменник", SERVER_RESOURCE, "REMOVE_EXCHANGE"))
            }
            val exchangeCode = if (dto.exchange != null) {
                "buttons.resources.exchange.change"
            } else {
                "buttons.resources.exchange.set"
            }
            val exchangeDefault = if (dto.exchange != null) {
                "💱 Изменить обменник"
            } else {
                "💱 Указать обменник"
            }
            add(button(1, locale, exchangeCode, exchangeDefault, SERVER_RESOURCE, "ADD_EXCHANGE"))
        }
    }

    private fun noteButtons(dto: ServerResourceDto, locale: Locale): List<InlineMarkupDataDto> = buildList {
        if (dto.main) {
            add(button(6, locale, "buttons.notes.add", "✍️ Добавить заметку", SERVER_RESOURCE, "ADD_NOTE"))
            if (dto.notes.isNotEmpty()) {
                add(button(6, locale, "buttons.notes.remove", "❌ Удалить заметку", SERVER_RESOURCE, "REMOVE_NOTE"))
            }
            add(button(7, locale, "buttons.resources.remove_main", "🚫 Отменить назначение основным", SERVER_RESOURCE, "REMOVE_MAIN"))
        } else if (!dto.hasMain) {
            add(button(6, locale, "buttons.resources.set_main", "👑 Сделать основным", SERVER_RESOURCE, "SET_MAIN"))
        }
    }

    private fun historyButtons(dto: ServerResourceDto, locale: Locale): List<InlineMarkupDataDto> {
        if (!dto.hasHistory) return emptyList()
        return listOf(button(98, locale, "buttons.resources.history", "🗒 Последние 20 операций", SERVER_RESOURCE, "RESOURCE_HISTORY"))
    }

    private fun notifyButtons(dto: ServerResourceDto, locale: Locale): List<InlineMarkupDataDto> {
        val code = if (dto.notifyDisable) {
            "buttons.resources.notify.resume"
        } else {
            "buttons.resources.notify.stop"
        }
        val default = if (dto.notifyDisable) {
            "❌ Продолжить ловлю"
        } else {
            "✅ Закончил ловить"
        }
        return listOf(button(97, locale, code, default, SERVER_RESOURCE, "DISABLE_NOTIFY"))
    }

    private fun backButton(locale: Locale): InlineMarkupDataDto =
        button(99, locale, "buttons.notes.back", "🔙 Вернуться", SERVER_RESOURCE, "BACK")

    private fun button(
        row: Int,
        locale: Locale,
        code: String,
        default: String,
        state: StateCode,
        payload: String = "",
        vararg args: Any?,
    ): InlineMarkupDataDto = InlineMarkupDataDto(
        rowPos = row,
        text = i18n(code, locale, default, *args),
        data = CallbackObject(state, payload)
    )
}
