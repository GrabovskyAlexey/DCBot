package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.resources

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ResourcesService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

@Component
class IncrementCbProcessor(
    userService: UserService,
    resourcesService: ResourcesService,
    stateService: StateService,
) : QuickResourceProcessor(userService, resourcesService, stateService, StateCode.ADD_CB) {
    override fun requiresCbEnabled(): Boolean = true
}
