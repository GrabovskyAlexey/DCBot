package ru.grabovsky.dungeoncrusherbot.strategy.flow.resources

import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class ResourcesPromptBuilder(
    private val messageSource: MessageSource,
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
        val base = messageSource.getMessage(baseKey, null, locale)
        val error = if (invalid) {
            "\n" + messageSource.getMessage("flow.resources.prompt.error.positive", null, locale)
        } else ""
        return ResourcesPromptModel(text = base + error)
    }

    fun exchangePrompt(locale: Locale, invalid: Boolean): ResourcesPromptModel {
        val base = messageSource.getMessage("flow.resources.prompt.text.exchange", null, locale)
        val error = if (invalid) {
            "\n" + messageSource.getMessage("flow.resources.prompt.error.not_blank", null, locale)
        } else ""
        return ResourcesPromptModel(text = base + error)
    }

    fun addNotePrompt(locale: Locale, invalid: Boolean): ResourcesPromptModel {
        val base = messageSource.getMessage("flow.resources.prompt.text.note_add", null, locale)
        val error = if (invalid) {
            "\n" + messageSource.getMessage("flow.resources.prompt.error.not_blank", null, locale)
        } else ""
        return ResourcesPromptModel(text = base + error)
    }

    fun removeNotePrompt(locale: Locale, notes: List<String>, invalid: Boolean): ResourcesPromptModel {
        val base = messageSource.getMessage("flow.resources.prompt.text.note_remove", null, locale)
        val error = if (invalid) {
            "\n" + messageSource.getMessage("flow.resources.prompt.error.note_range", null, locale)
        } else ""
        val enumerated = notes.mapIndexed { index, note -> "${index + 1}. $note" }
        return ResourcesPromptModel(text = base + error, notes = enumerated)
    }
}
