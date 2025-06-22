package ru.grabovsky.dungeoncrusherbot.strategy.message

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Component
class UpdateSubscribeMessage(
    messageGenerateService: MessageGenerateService,
    userService: UserService,
    serverService: ServerService
) : SubscribeMessage(messageGenerateService, userService, serverService)