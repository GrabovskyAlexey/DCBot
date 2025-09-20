package ru.grabovsky.dungeoncrusherbot.strategy.message.maze

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.MazeDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class MazeMessageTest : ShouldSpec({
    val messageService = mockk<MessageGenerateService>(relaxed = true)
    val message = MazeMessage(messageService)
    val user = mockk<TgUser>(relaxed = true)

    should("offer single-step controls when sameSteps disabled") {
        val buttons = message.inlineButtons(user, MazeDto(sameSteps = false))

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
        val buttons = message.inlineButtons(user, MazeDto(sameSteps = true))

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
