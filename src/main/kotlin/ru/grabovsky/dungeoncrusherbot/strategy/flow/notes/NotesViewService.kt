package ru.grabovsky.dungeoncrusherbot.strategy.flow.notes

import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import java.util.Locale
import org.telegram.telegrambots.meta.api.objects.User as TgUser

internal const val NOTES_LIMIT = 20

@Component
class NotesViewService(
    private val userService: UserService,
    private val messageSource: MessageSource,
) {

    fun buildOverview(user: TgUser, locale: Locale): NotesOverviewModel {
        val entity = userService.getUser(user.id)
        val notes = entity?.notes?.toList().orEmpty()
        val items = notes.mapIndexed { index, note -> NoteItem(index = index + 1, text = note) }
        val buttons = mutableListOf<NoteButton>()

        if (notes.size < NOTES_LIMIT) {
            buttons += button(
                row = 0,
                col = 0,
                code = "buttons.notes.add",
                default = "Add note",
                action = "ADD",
                locale = locale
            )
        }
        if (notes.isNotEmpty()) {
            buttons += button(
                row = 0,
                col = 1,
                code = "buttons.notes.remove",
                default = "Remove note",
                action = "REMOVE",
                locale = locale
            )
            buttons += button(
                row = 1,
                col = 0,
                code = "buttons.notes.clear",
                default = "Clear notes",
                action = "CLEAR",
                locale = locale
            )
        }

        return NotesOverviewModel(
            notes = items,
            buttons = buttons,
        )
    }

    private fun button(row: Int, col: Int, code: String, default: String, action: String, locale: Locale): NoteButton =
        NoteButton(
            label = messageSource.getMessage(code, null, default, locale) ?: default,
            action = action,
            row = row,
            col = col,
        )
}

data class NotesOverviewModel(
    val notes: List<NoteItem>,
    val buttons: List<NoteButton>,
)

data class NoteItem(
    val index: Int,
    val text: String,
)

data class NoteButton(
    val label: String,
    val action: String,
    val row: Int,
    val col: Int,
)
