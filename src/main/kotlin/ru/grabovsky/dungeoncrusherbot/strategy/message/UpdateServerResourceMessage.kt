package ru.grabovsky.dungeoncrusherbot.strategy.message

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService

@Component
class UpdateServerResourceMessage(messageGenerateService: MessageGenerateService, serverService: ServerService) :
    ServerResourceMessage(messageGenerateService)