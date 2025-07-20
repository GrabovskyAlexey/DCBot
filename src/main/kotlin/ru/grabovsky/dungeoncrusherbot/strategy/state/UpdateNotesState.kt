package ru.grabovsky.dungeoncrusherbot.strategy.state

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService

@Component
class UpdateNotesState(stateService: StateService) : NotesState(stateService)








