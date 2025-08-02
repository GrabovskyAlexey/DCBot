package ru.grabovsky.dungeoncrusherbot.strategy.processor.message.resources

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.message.MessageVerificationProcessor

@Component
class SellDraadorProcessor(stateService: StateService) : MessageVerificationProcessor(stateService)