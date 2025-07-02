package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Component
class UpdateNotifyProcessor(userService: UserService, stateService: StateService
): NotifyProcessor(userService, stateService)