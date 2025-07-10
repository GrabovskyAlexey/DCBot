package ru.grabovsky.dungeoncrusherbot.strategy.message

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService

@Component
class UpdateResourcesMessage(messageGenerateService: MessageGenerateService, serverService: ServerService) :
    ResourcesMessage(messageGenerateService, serverService)