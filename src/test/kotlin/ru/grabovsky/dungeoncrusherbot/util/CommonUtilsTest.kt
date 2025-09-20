package ru.grabovsky.dungeoncrusherbot.util

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.strategy.message.resources.ResourcesMessage
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.strategy.state.resources.ResourcesState

class CommonUtilsTest : ShouldSpec({
    should("derive StateCode from a State implementation") {
        with(CommonUtils) {
            ResourcesState().currentStateCode("State") shouldBe StateCode.RESOURCES
        }
    }

    should("derive StateCode from a Message implementation") {
        val message = ResourcesMessage(mockk<MessageGenerateService>(relaxed = true), mockk<ServerService>(relaxed = true))
        message.classStateCode() shouldBe StateCode.RESOURCES
    }
})
