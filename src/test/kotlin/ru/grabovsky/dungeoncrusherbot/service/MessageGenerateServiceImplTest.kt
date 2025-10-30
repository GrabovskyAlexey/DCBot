package ru.grabovsky.dungeoncrusherbot.service

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import freemarker.template.Template
import freemarker.template.Configuration
import java.io.StringWriter

class MessageGenerateServiceImplTest : ShouldSpec({
    val template = mockk<Template>()
    val configuration = mockk<Configuration>()
    val freeMarkerConfigurer = mockk<FreeMarkerConfigurer>()

    every { configuration.getTemplate("notification_siege.ftl") } returns template
    every { freeMarkerConfigurer.configuration } returns configuration
    every { template.process(any(), any<StringWriter>()) } answers {
        val writer = args[1] as StringWriter
        writer.write("rendered")
    }

    val service = MessageGenerateServiceImpl(freeMarkerConfigurer)

    should("render template with provided model data") {
        service.process(StateCode.NOTIFICATION_SIEGE, mapOf("key" to "value")) shouldBe "rendered"
    }
})
