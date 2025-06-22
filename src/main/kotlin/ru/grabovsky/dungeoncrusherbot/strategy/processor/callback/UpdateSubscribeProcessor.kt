package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Component
class UpdateSubscribeProcessor(
    userService: UserService,
    serverService: ServerService
): SubscribeProcessor(userService, serverService)