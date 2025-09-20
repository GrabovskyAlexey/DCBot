package ru.grabovsky.dungeoncrusherbot.strategy.state.subscribe

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class SubscribeStateTest : ShouldSpec({
    val user = mockk<TgUser>(relaxed = true)

    should("always transition to UPDATE_SUBSCRIBE") {
        SubscribeState().getNextState(user) shouldBe StateCode.UPDATE_SUBSCRIBE
        UpdateSubscribeState().getNextState(user) shouldBe StateCode.UPDATE_SUBSCRIBE
    }
})
