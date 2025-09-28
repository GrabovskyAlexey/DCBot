package ru.grabovsky.dungeoncrusherbot.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.framework.exception.StateNotFoundException
import ru.grabovsky.dungeoncrusherbot.repository.StateRepository
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class StateServiceImplTest : ShouldSpec({

    val repository = mockk<StateRepository>()
    val service = StateServiceImpl(repository)
    val user = mockk<TgUser>(relaxed = true) {
        every { id } returns 101L
        every { userName } returns "tester"
        every { firstName } returns "Tester"
    }

    beforeTest {
        clearMocks(repository)
    }

    should("create new state if user state does not exist") {
        every { repository.findByUserId(101L) } returns null
        every { repository.saveAndFlush(any()) } answers { firstArg() }

        service.updateState(user, StateCode.START, callbackData = "payload")

        verify { repository.saveAndFlush(match { it.userId == 101L && it.state == StateCode.START }) }
    }

    should("update existing state when present") {
        val existing = UserState(userId = 101L, state = StateCode.HELP)
        every { repository.findByUserId(101L) } returns existing
        every { repository.saveAndFlush(existing) } answers { firstArg() }

        service.updateState(user, StateCode.MAZE)

        existing.state shouldBe StateCode.MAZE
        verify { repository.saveAndFlush(existing) }
    }

    should("return state when it exists") {
        val state = UserState(userId = 101L, state = StateCode.START)
        every { repository.findByUserId(101L) } returns state

        service.getState(user) shouldBe state
    }

    should("throw when state is absent") {
        every { repository.findByUserId(101L) } returns null

        shouldThrow<StateNotFoundException> {
            service.getState(user)
        }
    }

    should("delegate saveState to repository") {
        val state = UserState(userId = 101L, state = StateCode.START)
        every { repository.saveAndFlush(state) } returns state

        service.saveState(state)

        verify { repository.saveAndFlush(state) }
    }
})
