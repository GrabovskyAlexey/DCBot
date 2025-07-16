package ru.grabovsky.dungeoncrusherbot.strategy.message

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.dto.ReplyMarkupDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.DataModel
import ru.grabovsky.dungeoncrusherbot.util.CommonUtils.currentStateCode

@Component
abstract class AbstractSendMessage<T: DataModel?>(private val messageGenerateService: MessageGenerateService) {

    fun classStateCode() = this.currentStateCode("Message")

    fun message(data: T? = null): String = messageGenerateService.process(classStateCode(), data)

    fun inlineButtons(user: User, data: T?): List<InlineMarkupDataDto> = emptyList()

    fun replyButtons(user: User, data: T? = null): List<ReplyMarkupDto> = emptyList()

    fun isPermitted(user: User): Boolean = true
}
