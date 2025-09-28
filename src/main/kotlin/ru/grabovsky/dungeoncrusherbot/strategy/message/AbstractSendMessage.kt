package ru.grabovsky.dungeoncrusherbot.strategy.message

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.dto.ReplyMarkupDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.DataModel
import ru.grabovsky.dungeoncrusherbot.util.CommonUtils.currentStateCode
import java.util.Locale

@Component
abstract class AbstractSendMessage<T: DataModel?>(private val messageGenerateService: MessageGenerateService) {

    @Autowired
    private lateinit var messageSource: MessageSource

    fun classStateCode() = this.currentStateCode("Message")

    fun message(user: User, locale: Locale, data: T? = null): String {
        return messageGenerateService.process(classStateCode(), data, locale)
    }

    fun inlineButtons(user: User, data: T?, locale: Locale): List<InlineMarkupDataDto> = emptyList()

    fun replyButtons(user: User, data: T? = null, locale: Locale): List<ReplyMarkupDto> = emptyList()

    fun isPermitted(user: User): Boolean = true

    protected fun i18n(code: String, locale: Locale, default: String? = null, vararg args: Any?): String {
        val arguments = if (args.isEmpty()) null else Array(args.size) { args[it] ?: "" }
        return messageSource.getMessage(code, arguments, default ?: code, locale)!!
    }
}
