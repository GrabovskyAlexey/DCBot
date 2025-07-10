package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Component
class UpdateResourcesProcessor(userService: UserService, stateService: StateService
): ResourcesProcessor(userService, stateService)