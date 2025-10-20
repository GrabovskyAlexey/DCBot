package ru.grabovsky.dungeoncrusherbot.util

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.message.HelpMessage
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.strategy.state.StartState

class CommonUtilsTest : ShouldSpec({
    should("derive StateCode from a State implementation") {
        with(CommonUtils) {
            StartState().currentStateCode("State") shouldBe StateCode.START
        }
    }

    should("derive StateCode from a Message implementation") {
        val message = HelpMessage(mockk<MessageGenerateService>(relaxed = true))
        message.classStateCode() shouldBe StateCode.HELP
    }
})
