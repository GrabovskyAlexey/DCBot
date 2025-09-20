package ru.grabovsky.dungeoncrusherbot.strategy.state.maze

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class MazeStateTest : ShouldSpec({
    val user = mockk<TgUser>(relaxed = true) { every { id } returns 802L }

    should("select next state based on callback data") {
        val stateService = mockk<StateService>()
        val userState = UserState(userId = 802L, state = StateCode.MAZE)
        every { stateService.getState(user) } returns userState

        val state = MazeState(stateService)
        userState.callbackData = "REFRESH_MAZE"
        state.getNextState(user) shouldBe StateCode.CONFIRM_REFRESH_MAZE

        userState.callbackData = "SAME_LEFT"
        state.getNextState(user) shouldBe StateCode.SAME_LEFT

        userState.callbackData = "SAME_RIGHT"
        state.getNextState(user) shouldBe StateCode.SAME_RIGHT

        userState.callbackData = "SAME_CENTER"
        state.getNextState(user) shouldBe StateCode.SAME_CENTER

        userState.callbackData = "UNKNOWN"
        state.getNextState(user) shouldBe StateCode.UPDATE_MAZE
    }

    should("return VERIFY for same-step states and UPDATE_MAZE after confirm") {
        SameLeftState().getNextState(user) shouldBe StateCode.VERIFY
        SameRightState().getNextState(user) shouldBe StateCode.VERIFY
        SameCenterState().getNextState(user) shouldBe StateCode.VERIFY
        ConfirmRefreshMazeState().getNextState(user) shouldBe StateCode.UPDATE_MAZE
    }
})
