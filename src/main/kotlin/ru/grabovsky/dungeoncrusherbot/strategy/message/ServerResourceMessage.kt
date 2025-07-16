package ru.grabovsky.dungeoncrusherbot.strategy.message

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerResourceDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

@Component
class ServerResourceMessage(messageGenerateService: MessageGenerateService) :
    AbstractSendMessage<ServerResourceDto>(messageGenerateService) {
    override fun inlineButtons(
        user: User,
        data: ServerResourceDto?
    ): List<InlineMarkupDataDto> {
        return mutableListOf(
            InlineMarkupDataDto(
                rowPos = 2,
                text = "\uD83E\uDE86 Поймать",
                data = CallbackObject(StateCode.SERVER_RESOURCE, "ADD_DRAADOR")
            ),
            InlineMarkupDataDto(
                rowPos = 2,
                text = "\uD83E\uDE86 Продать",
                data = CallbackObject(StateCode.SERVER_RESOURCE, "SELL_DRAADOR")
            ),

            InlineMarkupDataDto(
                rowPos = 4,
                text = "\uD83D\uDFE3 Добавить",
                data = CallbackObject(StateCode.SERVER_RESOURCE, "ADD_VOID")
            ),
            InlineMarkupDataDto(
                rowPos = 4,
                text = "\uD83D\uDFE3 Удалить",
                data = CallbackObject(StateCode.SERVER_RESOURCE, "REMOVE_VOID")
            ),
            InlineMarkupDataDto(
                rowPos = 97,
                text = if (data?.notifyDisable == true) "✅ Включить уведомления" else "❌ Отключить уведомления",
                data = CallbackObject(StateCode.SERVER_RESOURCE, "DISABLE_NOTIFY")
            ),
            InlineMarkupDataDto(
                rowPos = 99,
                text = "\uD83D\uDD19 Вернуться",
                data = CallbackObject(StateCode.SERVER_RESOURCE, "BACK")
            )
        ).also {
            if (data?.exchange != null && !data.isMain) {
                it.add(
                    InlineMarkupDataDto(
                        rowPos = 1,
                        text = "\uD83D\uDCB1 Удалить обменник",
                        data = CallbackObject(StateCode.SERVER_RESOURCE, "REMOVE_EXCHANGE")
                    )
                )
            }
            if (data?.isMain != true) {
                it.addAll(
                    listOf(
                        InlineMarkupDataDto(
                            rowPos = 1,
                            text = if (data?.exchange != null) "\uD83D\uDCB1 Изменить обменник" else "\uD83D\uDCB1 Указать обменник",
                            data = CallbackObject(StateCode.SERVER_RESOURCE, "ADD_EXCHANGE")
                        ),
                        InlineMarkupDataDto(
                            rowPos = 3,
                            text = "\uD83E\uDE86 Получить",
                            data = CallbackObject(StateCode.SERVER_RESOURCE, "RECEIVE_DRAADOR")
                        ),
                        InlineMarkupDataDto(
                            rowPos = 3,
                            text = "\uD83E\uDE86 Передать",
                            data = CallbackObject(StateCode.SERVER_RESOURCE, "SEND_DRAADOR")
                        ),
                        InlineMarkupDataDto(
                            rowPos = 5,
                            text = "✅ Сделать основным",
                            data = CallbackObject(StateCode.SERVER_RESOURCE, "SET_MAIN")
                        ),
                    )
                )
            }
            if (data?.hasHistory == true) {
                it.add(
                    InlineMarkupDataDto(
                        rowPos = 98,
                        text = "\uD83D\uDDD2 Последние 20 операций",
                        data = CallbackObject(StateCode.SERVER_RESOURCE, "RESOURCE_HISTORY")
                    )
                )
            }
        }
    }
}