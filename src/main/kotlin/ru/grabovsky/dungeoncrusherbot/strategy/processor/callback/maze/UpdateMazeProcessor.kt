package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.maze

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MazeService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Component
class UpdateMazeProcessor(userService: UserService, mazeService: MazeService, stateService: StateService
): MazeProcessor(userService, mazeService, stateService)