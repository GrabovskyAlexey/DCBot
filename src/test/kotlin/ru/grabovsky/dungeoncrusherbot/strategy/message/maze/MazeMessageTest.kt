package ru.grabovsky.dungeoncrusherbot.strategy.message.maze

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.every
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import org.springframework.context.MessageSource
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.setTestMessageSource
import ru.grabovsky.dungeoncrusherbot.strategy.dto.MazeDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.util.Locale
import java.text.MessageFormat
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class MazeMessageTest : ShouldSpec({
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
    val message = MazeMessage(messageService).apply { setTestMessageSource(messageSource) }
    val user = mockk<TgUser>(relaxed = true)

    should("offer single-step controls when sameSteps disabled") {
        val buttons = message.inlineButtons(user, MazeDto(sameSteps = false), Locale.forLanguageTag("ru"))

        buttons.shouldHaveSize(6)
        buttons.map { it.data }.shouldContainAll(
            CallbackObject(StateCode.UPDATE_MAZE, "LEFT"),
            CallbackObject(StateCode.UPDATE_MAZE, "CENTER"),
            CallbackObject(StateCode.UPDATE_MAZE, "RIGHT"),
            CallbackObject(StateCode.UPDATE_MAZE, "SAME_STEPS"),
            CallbackObject(StateCode.UPDATE_MAZE, "HISTORY"),
            CallbackObject(StateCode.UPDATE_MAZE, "REFRESH_MAZE"),
        )
        buttons.first { it.data.data == "REFRESH_MAZE" }.rowPos shouldBe 4
    }

    should("offer repeated-step controls when sameSteps enabled") {
        val buttons = message.inlineButtons(user, MazeDto(sameSteps = true), Locale.forLanguageTag("ru"))

        buttons.shouldHaveSize(6)
        buttons.map { it.data }.shouldContainAll(
            CallbackObject(StateCode.UPDATE_MAZE, "SAME_LEFT"),
            CallbackObject(StateCode.UPDATE_MAZE, "SAME_CENTER"),
            CallbackObject(StateCode.UPDATE_MAZE, "SAME_RIGHT"),
            CallbackObject(StateCode.UPDATE_MAZE, "SAME_STEPS"),
            CallbackObject(StateCode.UPDATE_MAZE, "HISTORY"),
            CallbackObject(StateCode.UPDATE_MAZE, "REFRESH_MAZE"),
        )
        buttons.map { it.data }.shouldNotContain(CallbackObject(StateCode.UPDATE_MAZE, "LEFT"))
        buttons.first { it.data.data == "SAME_STEPS" }.rowPos shouldBe 2
    }
})
