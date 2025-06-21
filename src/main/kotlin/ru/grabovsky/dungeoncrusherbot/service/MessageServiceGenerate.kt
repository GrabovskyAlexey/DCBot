package ru.grabovsky.dungeoncrusherbot.service

import org.springframework.stereotype.Service
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer
import ru.grabovsky.dungeoncrusherbot.bot.commands.State
import java.io.StringWriter

@Service
class MessageServiceGenerate(
    private val freeMarkerConfigurer: FreeMarkerConfigurer,
) {
    fun process(state: State, freemarkerData: Any?): String {
        val template = state.name.lowercase()
        return processed(freemarkerData?.let { mapOf("data" to it) }?: emptyMap(), "$template.ftl")
    }

    private fun processed(data: Map<String, Any>, templateName: String): String {
        val template = freeMarkerConfigurer.configuration.getTemplate(templateName)
        val output = StringWriter()
        template.process(data, output)
        return output.toString()
    }
}