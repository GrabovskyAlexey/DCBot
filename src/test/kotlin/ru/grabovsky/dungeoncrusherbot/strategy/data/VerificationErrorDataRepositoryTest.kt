package ru.grabovsky.dungeoncrusherbot.strategy.data

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.entity.VerificationRequest
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.VerificationErrorDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class VerificationErrorDataRepositoryTest : ShouldSpec({
    val stateService = mockk<StateService>()
    val repository = VerificationErrorDataRepository(stateService)
    val tgUser = mockk<TgUser>(relaxed = true) { every { id } returns 2000L }

    fun stub(stateCode: StateCode?) {
        val state = UserState(userId = 2000L, state = StateCode.VERIFY)
        state.verification = stateCode?.let { VerificationRequest(message = "", stateCode = it) }
        every { stateService.getState(tgUser) } returns state
    }

    should("produce messages for various invalid inputs") {
        listOf(
            StateCode.SAME_LEFT,
            StateCode.SAME_CENTER,
            StateCode.SAME_RIGHT,
        ).forEach { code ->
            stub(code)
            val dto = repository.getData(tgUser)
            dto shouldBe VerificationErrorDto(dto.message)
            dto.message.isNotBlank().shouldBeTrue()
        }
    }

    should("return default message when verification result absent") {
        stub(null)
        val dto = repository.getData(tgUser)
        dto.message.isNotBlank().shouldBeTrue()
    }
})


