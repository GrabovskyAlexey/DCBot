package ru.grabovsky.dungeoncrusherbot.strategy.message.notes

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.mockk.mockk
import io.mockk.every
import org.springframework.context.MessageSource
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.NotesDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.setTestMessageSource
import java.util.Locale
import java.text.MessageFormat
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class NotesMessageTest : ShouldSpec({
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
    val message = NotesMessage(messageService).apply { setTestMessageSource(messageSource) }
    val user = mockk<TgUser>(relaxed = true)

    should("offer add button when note list below limit") {
        val dto = NotesDto(notes = emptyList())

        val buttons = message.inlineButtons(user, dto, Locale.forLanguageTag("ru"))

        buttons.map { it.data }.shouldContain(CallbackObject(StateCode.UPDATE_NOTES, "ADD_NOTE"))
        buttons.map { it.data.data }.shouldNotContain("REMOVE_NOTE")
        buttons.map { it.data.data }.shouldNotContain("CLEAR_NOTES")
    }

    should("offer remove and clear options when list is full and coming from server view") {
        val dto = NotesDto(notes = (1..20).map { "note $it" }, fromServer = true)

        val buttons = message.inlineButtons(user, dto, Locale.forLanguageTag("ru"))

        buttons.map { it.data.data }.shouldNotContain("ADD_NOTE")
        buttons.map { it.data }.shouldContainExactly(
            CallbackObject(StateCode.UPDATE_NOTES, "REMOVE_NOTE"),
            CallbackObject(StateCode.UPDATE_NOTES, "CLEAR_NOTES")
        )
        buttons.map { it.data.data }.shouldNotContain("BACK")
    }
})
