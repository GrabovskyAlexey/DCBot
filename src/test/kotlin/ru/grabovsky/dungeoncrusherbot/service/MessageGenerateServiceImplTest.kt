package ru.grabovsky.dungeoncrusherbot.service

import freemarker.template.Configuration
import freemarker.template.Template
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer
import java.io.StringWriter
import java.util.*

class MessageGenerateServiceImplTest : ShouldSpec({
    val template = mockk<Template>()
    val configuration = mockk<Configuration>()
    val freeMarkerConfigurer = mockk<FreeMarkerConfigurer>()

    every { freeMarkerConfigurer.configuration } returns configuration
    every { configuration.getTemplate("notification/mine.ftl", any<Locale>()) } returns template
    every { template.process(any(), any<StringWriter>()) } answers {
        val writer = args[1] as StringWriter
        writer.write("rendered")
    }

    val service = MessageGenerateServiceImpl(freeMarkerConfigurer)

    should("render template with provided model data and locale") {
        service.processTemplate("notification/mine", mapOf("key" to "value"), Locale.ENGLISH) shouldBe "rendered"

        verify { configuration.getTemplate("notification/mine.ftl", Locale.ENGLISH) }
    }
})
