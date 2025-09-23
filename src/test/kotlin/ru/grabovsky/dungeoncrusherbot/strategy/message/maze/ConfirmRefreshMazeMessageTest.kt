package ru.grabovsky.dungeoncrusherbot.strategy.message.maze

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.every
import org.springframework.context.MessageSource
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.setTestMessageSource
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.util.Locale
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class ConfirmRefreshMazeMessageTest : ShouldSpec({
    val messageService = mockk<MessageGenerateService>(relaxed = true)
    val messageSource = mockk<MessageSource> {
        every { getMessage(any(), any(), any(), any()) } answers { invocation ->
            val args = invocation.invocation.args
            val code = args[0] as String
            val default = args[2] as String?
            default ?: code
        }
    }
    val message = ConfirmRefreshMazeMessage(messageService).apply { setTestMessageSource(messageSource) }
    val user = mockk<TgUser>(relaxed = true)

    should("offer confirm and cancel buttons for maze reset") {
        val buttons = message.inlineButtons(user, null, Locale.forLanguageTag("ru"))

        buttons.map { it.data }.shouldContainExactly(
            CallbackObject(StateCode.CONFIRM_REFRESH_MAZE, "CONFIRM"),
            CallbackObject(StateCode.CONFIRM_REFRESH_MAZE, "NOT_CONFIRM"),
        )
        buttons.all { it.rowPos == 1 } shouldBe true
    }
})
