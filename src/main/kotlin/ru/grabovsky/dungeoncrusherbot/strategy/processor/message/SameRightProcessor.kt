package ru.grabovsky.dungeoncrusherbot.strategy.processor.message

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService

@Component
class SameRightProcessor(stateService: StateService) : MessageVerificationProcessor(stateService)