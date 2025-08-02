package ru.grabovsky.dungeoncrusherbot.strategy.state.maze

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService

@Component
class UpdateMazeState(stateService: StateService) : MazeState(stateService)