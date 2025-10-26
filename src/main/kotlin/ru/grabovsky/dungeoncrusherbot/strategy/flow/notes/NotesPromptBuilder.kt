package ru.grabovsky.dungeoncrusherbot.strategy.flow.notes

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import java.util.Locale

@Component
class NotesPromptBuilder(
    private val i18nService: I18nService,
) {
    fun addPrompt(locale: Locale, invalid: Boolean = false): NotesPromptModel {
        val base = i18nService.i18n("flow.notes.prompt.text.add", locale, "Введите текст заметки")
        val error = if (invalid) "\n${i18nService.i18n("flow.notes.prompt.error.not_blank", locale, "Текст не должен быть пустым")}" else ""
        return NotesPromptModel(text = base + error)
    }

    fun removePrompt(locale: Locale, notes: List<String>, invalid: Boolean = false): NotesPromptModel {
        val enumerated = notes.mapIndexed { index, note -> "${index + 1}. $note" }
        val base = i18nService.i18n("flow.notes.prompt.text.remove", locale, "Введите номер заметки для удаления")
        val error = if (invalid) "\n${i18nService.i18n("flow.notes.prompt.error.range", locale, "Введите корректный номер")}" else ""
        return NotesPromptModel(text = base + error, notes = enumerated)
    }
}

data class NotesPromptModel(
    val text: String,
    val notes: List<String> = emptyList(),
)
