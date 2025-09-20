package ru.grabovsky.dungeoncrusherbot.strategy.data

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.entity.VerificationRequest
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class VerificationErrorDataRepositoryTest : ShouldSpec({
    val userService = mockk<UserService>()
    val stateService = mockk<StateService>()
    val repository = VerificationErrorDataRepository(userService, stateService)
    val tgUser = mockk<TgUser>(relaxed = true) { every { id } returns 2000L }

    fun stub(stateCode: StateCode?) {
        val state = UserState(userId = 2000L, state = StateCode.VERIFY)
        state.verification = stateCode?.let { VerificationRequest(message = "", stateCode = it) }
        every { stateService.getState(tgUser) } returns state
    }

    should("возвращать сообщение для разных состояний") {
        listOf(StateCode.ADD_DRAADOR, StateCode.ADD_EXCHANGE, StateCode.ADD_NOTE).forEach { code ->
            stub(code)
            repository.getData(tgUser).message.isNotBlank().shouldBeTrue()
        }
    }

    should("возвращать сообщение по умолчанию если данных нет") {
        stub(null)
        repository.getData(tgUser).message.isNotBlank().shouldBeTrue()
    }
})
