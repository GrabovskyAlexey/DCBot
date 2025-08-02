package ru.grabovsky.dungeoncrusherbot.strategy.state.resources

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService

@Component
class UpdateServerResourceState(stateService: StateService) : ServerResourceState(stateService)








