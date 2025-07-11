package ru.grabovsky.dungeoncrusherbot.strategy.data

import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Repository
class UpdateMazeDataRepository(userService: UserService, stateService: StateService)
    : MazeDataRepository(userService, stateService)