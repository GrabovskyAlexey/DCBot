package ru.grabovsky.dungeoncrusherbot.strategy.message.notes

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.NotesDto
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.util.Locale

@Component
class NotesMessage(
    messageGenerateService: MessageGenerateService,
) : AbstractSendMessage<NotesDto>(messageGenerateService) {
    override fun inlineButtons(user: User, data: NotesDto?, locale: Locale): List<InlineMarkupDataDto> {
        val result: MutableList<InlineMarkupDataDto> = mutableListOf()
        if((data?.notes?.size ?: 0) < 20) {
            result.add(
                InlineMarkupDataDto(
                    rowPos = 1,
                    text = i18n(
                        code = "buttons.notes.add",
                        locale = locale,
                        default = "✍\uFE0F Добавить заметку"
                    ),
                    data = CallbackObject(
                        StateCode.UPDATE_NOTES, "ADD_NOTE"
                    )
                )
            )
        }
        if(data?.notes?.isNotEmpty() == true) {
            result.add(
                InlineMarkupDataDto(
                    rowPos = 2,
                    text = i18n(
                        code = "buttons.notes.remove",
                        locale = locale,
                        default = "❌ Удалить заметку"
                    ),
                    data = CallbackObject(
                        StateCode.UPDATE_NOTES, "REMOVE_NOTE"
                    )
                )
            )
        }
        if(data?.notes?.isNotEmpty() == true) {
            result.add(
                InlineMarkupDataDto(
                    rowPos = 3,
                    text = i18n(
                        code = "buttons.notes.clear",
                        locale = locale,
                        default = "\uD83D\uDDD1 Удалить все заметки"
                    ),
                    data = CallbackObject(
                        StateCode.UPDATE_NOTES, "CLEAR_NOTES"
                    )
                )
            )
        }
        return result
    }
}
