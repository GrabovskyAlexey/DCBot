package ru.grabovsky.dungeoncrusherbot.strategy.state.subscribe

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.telegram.telegrambots.meta.api.objects.User as TgUser
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

class SubscribeStateTest : ShouldSpec({
    val user = mockk<TgUser>(relaxed = true)

    should("возвращать UPDATE_SUBSCRIBE для состояний подписок") {
        SubscribeState().getNextState(user) shouldBe StateCode.UPDATE_SUBSCRIBE
        UpdateSubscribeState().getNextState(user) shouldBe StateCode.UPDATE_SUBSCRIBE
    }
})
