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

        should("throw when state is absent") {
        every { repository.findByUserId(101L) } returns null

        shouldThrow<StateNotFoundException> {
            service.getState(user)
        }
    }
})
