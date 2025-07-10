package ru.grabovsky.dungeoncrusherbot.strategy.processor.message

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService

@Component
class AddDraadorProcessor(stateService: StateService) : MessageVerificationProcessor(stateService)