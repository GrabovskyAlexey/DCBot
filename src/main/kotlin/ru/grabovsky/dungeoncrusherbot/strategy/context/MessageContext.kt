package ru.grabovsky.dungeoncrusherbot.strategy.context

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.MessageModelDto
import ru.grabovsky.dungeoncrusherbot.strategy.data.AbstractDataRepository
import ru.grabovsky.dungeoncrusherbot.strategy.dto.DataModel
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.util.Locale

@Component
class MessageContext<T : DataModel>(
    private val sendMessages: Map<StateCode, AbstractSendMessage<T>>,
    private val abstractDataRepository: List<AbstractDataRepository<T>>
) {
    fun getMessage(user: User, stateCode: StateCode, locale: Locale): MessageModelDto? {
        return sendMessages[stateCode]
            ?.takeIf { it.isPermitted(user) }
            ?.let {
                val data = getData(user, stateCode)
                MessageModelDto(
                    message = it.message(user, locale, data),
                    inlineButtons = it.inlineButtons(user, data, locale),
                    replyButtons = it.replyButtons(user, data, locale)
                )
            }
    }

    private fun getData(user: User, stateCode: StateCode) =
        abstractDataRepository.firstOrNull { it.isAvailableForCurrentState(stateCode) }?.getData(user)
}
