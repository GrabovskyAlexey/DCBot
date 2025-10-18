package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.note

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Component
class UpdateNotesProcessor(stateService: StateService, userService: UserService): NotesProcessor(stateService, userService)