package ru.grabovsky.dungeoncrusherbot.strategy.processor.message.note

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.message.MessageVerificationProcessor

@Component
class AddNoteProcessor(stateService: StateService) : MessageVerificationProcessor(stateService)