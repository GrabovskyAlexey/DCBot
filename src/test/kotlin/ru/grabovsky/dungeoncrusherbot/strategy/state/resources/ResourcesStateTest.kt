package ru.grabovsky.dungeoncrusherbot.strategy.state.resources

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class ResourcesStateTest : ShouldSpec({
    val telegramUser = mockk<TgUser>(relaxed = true) { every { id } returns 800L }

    should("return VERIFY for resource operations that require confirmation") {
        val verifyStates = listOf(
            AddCbState(), AddDraadorState(), AddExchangeState(), AddVoidState(),
            RemoveCbState(), RemoveVoidState(), ReceiveDraadorState(),
            SellDraadorState(), SendDraadorState()
        )

        verifyStates.forEach { state ->
            state.getNextState(telegramUser) shouldBe StateCode.VERIFY
        }
    }

    should("return UPDATE_SERVER_RESOURCE for quick update states") {
        val updateStates = listOf(
            RemoveExchangeState(), IncrementCbState(), IncrementDraadorState(), IncrementVoidState(),
            DecrementCbState(), DecrementDraadorState(), DecrementVoidState(),
            QuickReceiveDraadorState(), QuickSendDraadorState()
        )

        updateStates.forEach { state ->
            state.getNextState(telegramUser) shouldBe StateCode.UPDATE_SERVER_RESOURCE
        }
    }

    should("return SERVER_RESOURCE for base resources state") {
        ResourcesState().getNextState(telegramUser) shouldBe StateCode.SERVER_RESOURCE
    }

    should("derive next state from ServerResourceState callback value") {
        val stateService = mockk<StateService>()
        val userState = UserState(userId = 800L, state = StateCode.SERVER_RESOURCE, callbackData = "REMOVE_EXCHANGE")
        every { stateService.getState(telegramUser) } returns userState

        val serverState = ServerResourceState(stateService)
        serverState.getNextState(telegramUser) shouldBe StateCode.REMOVE_EXCHANGE

        userState.callbackData = "BACK"
        serverState.getNextState(telegramUser) shouldBe StateCode.UPDATE_RESOURCES

        userState.callbackData = "UNKNOWN"
        serverState.getNextState(telegramUser) shouldBe StateCode.SERVER_RESOURCE

        verify(exactly = 3) { stateService.getState(telegramUser) }
    }
})
