package ru.grabovsky.dungeoncrusherbot.strategy.message.maze

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class ConfirmRefreshMazeMessageTest : ShouldSpec({
    val messageService = mockk<MessageGenerateService>(relaxed = true)
    val message = ConfirmRefreshMazeMessage(messageService)
    val user = mockk<TgUser>(relaxed = true)

    should("предлагать подтверждение и отмену сброса лабиринта") {
        val buttons = message.inlineButtons(user, null)

        buttons.map { it.data }.shouldContainExactly(
            CallbackObject(StateCode.CONFIRM_REFRESH_MAZE, "CONFIRM"),
            CallbackObject(StateCode.CONFIRM_REFRESH_MAZE, "NOT_CONFIRM"),
        )
        buttons.all { it.rowPos == 1 } shouldBe true
    }
})
