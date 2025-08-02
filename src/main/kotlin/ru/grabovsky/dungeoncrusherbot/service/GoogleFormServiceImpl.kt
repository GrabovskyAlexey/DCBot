package ru.grabovsky.dungeoncrusherbot.service

import org.springframework.stereotype.Service
import ru.grabovsky.dungeoncrusherbot.client.GoogleFormClient
import ru.grabovsky.dungeoncrusherbot.service.interfaces.GoogleFormService

@Service
class GoogleFormServiceImpl(
    private val googleFormClient: GoogleFormClient
) : GoogleFormService {
    override fun sendDraadorCount(count: String, discordName: String) {
        val formData = mapOf(
            "entry.449666117" to count,
            "entry.1851349150" to discordName
        )
        googleFormClient.submitForm(formData)
    }
}