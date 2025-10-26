package ru.grabovsky.dungeoncrusherbot.strategy.processor.message

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.message.maze.SameCenterProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.message.maze.SameLeftProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.message.maze.SameRightProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser
import org.telegram.telegrambots.meta.api.objects.message.Message

class MessageVerificationProcessorTest : ShouldSpec({
    val telegramUser = mockk<TgUser>(relaxed = true) { every { id } returns 900L }
    val message = mockk<Message>(relaxed = true) { every { text } returns "42" }

    data class Case(
        val stateCode: StateCode,
        val factory: (StateService) -> MessageProcessor
    )

    val cases = listOf(
        Case(StateCode.SAME_LEFT) { SameLeftProcessor(it) },
        Case(StateCode.SAME_RIGHT) { SameRightProcessor(it) },
        Case(StateCode.SAME_CENTER) { SameCenterProcessor(it) },
    )

    cases.forEach { case ->
        should("сохранять VerificationRequest для состояния ${case.stateCode}") {
            val stateService = mockk<StateService>()
            val userState = UserState(userId = 900L, state = case.stateCode)
            every { stateService.getState(telegramUser) } returns userState
            every { stateService.saveState(userState) } returns userState

            val processor = case.factory(stateService)
            processor.execute(telegramUser, message)

            userState.verification?.message shouldBe "42"
            userState.verification?.stateCode shouldBe case.stateCode
            verify { stateService.saveState(userState) }
        }
    }
})
