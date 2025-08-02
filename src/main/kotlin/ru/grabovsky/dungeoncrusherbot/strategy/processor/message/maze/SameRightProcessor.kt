package ru.grabovsky.dungeoncrusherbot.strategy.processor.message.maze

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.message.MessageVerificationProcessor

@Component
class SameRightProcessor(stateService: StateService) : MessageVerificationProcessor(stateService)