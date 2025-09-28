package ru.grabovsky.dungeoncrusherbot.strategy.message.settings

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import io.mockk.every
import org.springframework.context.MessageSource
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.SettingsDto
import ru.grabovsky.dungeoncrusherbot.setTestMessageSource
import java.util.Locale
import java.text.MessageFormat
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class SettingsMessageTest : ShouldSpec({
    val messageService = mockk<MessageGenerateService>(relaxed = true)
    val messageSource = mockk<MessageSource> {
        every { getMessage(any(), any(), any(), any()) } answers { invocation ->
            val args = invocation.invocation.args
            val rawArgs = args[1]
            val messageArgs = if (rawArgs is Array<*>) Array<Any?>(rawArgs.size) { rawArgs[it] } else emptyArray<Any?>()
            val default = args[2] as String?
            val locale = args[3] as Locale
            val patternMessage = default ?: args[0] as String
            MessageFormat(patternMessage, locale).format(messageArgs)
        }
    }
    val message = SettingsMessage(messageService).apply { setTestMessageSource(messageSource) }
    val user = mockk<TgUser>(relaxed = true)

    fun buttons(dto: SettingsDto) = message.inlineButtons(user, dto, Locale.forLanguageTag("ru"))
    fun text(data: String, buttons: List<InlineMarkupDataDto>) = buttons.first { it.data.data == data }.text

    should("менять текст только для затронутой настройки") {
        val base = buttons(SettingsDto(false, false, false, false))

        val siege = buttons(SettingsDto(true, false, false, false))
        text("NOTIFY_SIEGE", siege) shouldNotBe text("NOTIFY_SIEGE", base)
        text("NOTIFY_MINE", siege) shouldBe text("NOTIFY_MINE", base)

        val mine = buttons(SettingsDto(false, true, false, false))
        text("NOTIFY_MINE", mine) shouldNotBe text("NOTIFY_MINE", base)
        text("NOTIFY_SIEGE", mine) shouldBe text("NOTIFY_SIEGE", base)

        val cb = buttons(SettingsDto(false, false, true, false))
        text("CB_ENABLE", cb) shouldNotBe text("CB_ENABLE", base)
        text("QUICK_RESOURCES", cb) shouldBe text("QUICK_RESOURCES", base)

        val quick = buttons(SettingsDto(false, false, false, true))
        text("QUICK_RESOURCES", quick) shouldNotBe text("QUICK_RESOURCES", base)
        text("CB_ENABLE", quick) shouldBe text("CB_ENABLE", base)

        text("SEND_REPORT", quick) shouldBe text("SEND_REPORT", base)
        quick.first { it.data.data == "SEND_REPORT" }.rowPos shouldBe 99
    }
})
