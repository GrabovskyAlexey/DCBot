package ru.grabovsky.dungeoncrusherbot.strategy.processor.message

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService

@Component
class RemoveVoidProcessor(stateService: StateService) : MessageVerificationProcessor(stateService)