package ru.grabovsky.dungeoncrusherbot.strategy.message

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.MazeDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.NotesDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

@Component
class NotesMessage(
    messageGenerateService: MessageGenerateService,
) : AbstractSendMessage<NotesDto>(messageGenerateService) {
    override fun inlineButtons(
        user: User,
        data: NotesDto?
    ): List<InlineMarkupDataDto> {
        val result: MutableList<InlineMarkupDataDto> = mutableListOf()
        if((data?.notes?.size ?: 0) < 20) {
            result.add(
                InlineMarkupDataDto(
                    rowPos = 1,
                    text = "Добавить заметку",
                    data = CallbackObject(
                        StateCode.UPDATE_NOTES,"ADD_NOTE")
                )
            )
        }
        if(data?.notes?.isNotEmpty() == true) {
            result.add(
                InlineMarkupDataDto(
                    rowPos = 2,
                    text = "Удалить заметку",
                    data = CallbackObject(
                        StateCode.UPDATE_NOTES,"REMOVE_NOTE")
                )
            )
        }
        if(data?.fromServer == true) {
            result.add(
                InlineMarkupDataDto(
                    rowPos = 99,
                    text = "\uD83D\uDD19 Вернуться",
                    data = CallbackObject(StateCode.SERVER_RESOURCE, "BACK")
                )
            )
        }

        return result
    }
}