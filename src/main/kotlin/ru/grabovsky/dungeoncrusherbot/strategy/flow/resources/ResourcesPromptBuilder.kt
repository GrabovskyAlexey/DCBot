package ru.grabovsky.dungeoncrusherbot.strategy.flow.resources

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import java.util.*

@Component
class ResourcesPromptBuilder(
    private val i18nService: I18nService,
) {
    fun amountPrompt(locale: Locale, type: AmountActionType, invalid: Boolean = false): ResourcesPromptModel {
        val baseKey = when (type) {
            AmountActionType.ADD_DRAADOR -> "flow.resources.prompt.amount.add_draador"
            AmountActionType.SELL_DRAADOR -> "flow.resources.prompt.amount.sell_draador"
            AmountActionType.SEND_DRAADOR -> "flow.resources.prompt.amount.send_draador"
            AmountActionType.RECEIVE_DRAADOR -> "flow.resources.prompt.amount.receive_draador"
            AmountActionType.ADD_VOID -> "flow.resources.prompt.amount.add_void"
            AmountActionType.REMOVE_VOID -> "flow.resources.prompt.amount.remove_void"
            AmountActionType.ADD_CB -> "flow.resources.prompt.amount.add_cb"
            AmountActionType.REMOVE_CB -> "flow.resources.prompt.amount.remove_cb"
        }
        return buildResourcePromptModel(baseKey, "flow.resources.prompt.error.positive", invalid, locale)
    }

    fun exchangePrompt(locale: Locale, invalid: Boolean): ResourcesPromptModel =
        buildResourcePromptModel("flow.resources.prompt.text.exchange", "flow.resources.prompt.error.not_blank", invalid, locale)

    fun addNotePrompt(locale: Locale, invalid: Boolean): ResourcesPromptModel =
        buildResourcePromptModel("flow.resources.prompt.text.note_add", "flow.resources.prompt.error.not_blank", invalid, locale)


    fun removeNotePrompt(locale: Locale, notes: List<String>, invalid: Boolean): ResourcesPromptModel {
        val enumerated = notes.mapIndexed { index, note -> "${index + 1}. $note" }
        return buildResourcePromptModel("flow.resources.prompt.text.note_remove", "flow.resources.prompt.error.note_range", invalid, locale, enumerated)
    }

    private fun buildResourcePromptModel(baseKey: String, errorKey: String, invalid: Boolean, locale: Locale, notes: List<String>? = null): ResourcesPromptModel {
            val base = i18nService.i18n(baseKey, locale)
            val error = if (invalid) { "\n${i18nService.i18n(errorKey, locale)}" } else ""
            return ResourcesPromptModel(text = "$base$error")
    }
}
