package ru.grabovsky.dungeoncrusherbot.service

import org.springframework.stereotype.Service
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import java.io.StringWriter
import java.util.Locale

@Service
class MessageGenerateServiceImpl(
    private val freeMarkerConfigurer: FreeMarkerConfigurer,
): MessageGenerateService {
    override fun process(state: StateCode, freemarkerData: Any?, locale: Locale): String {
        val template = state.template ?: state.name.lowercase()
        val model = mutableMapOf<String, Any>("locale" to locale)
        if (freemarkerData != null) {
            model["data"] = freemarkerData
        }
        return processed(model, "$template.ftl")
    }

    private fun processed(data: Map<String, Any>, templateName: String): String {
        val template = freeMarkerConfigurer.configuration.getTemplate(templateName)
        val output = StringWriter()
        template.process(data, output)
        return output.toString()
    }
}
