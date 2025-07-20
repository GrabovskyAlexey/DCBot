package ru.grabovsky.dungeoncrusherbot.strategy.message

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService

@Component
class UpdateSettingsMessage(messageGenerateService: MessageGenerateService) : SettingsMessage(messageGenerateService)