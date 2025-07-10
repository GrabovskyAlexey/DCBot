package ru.grabovsky.dungeoncrusherbot.strategy.data

import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Repository
class UpdateServerResourceDataRepository(userService: UserService, stateService: StateService): ServerResourceDataRepository(userService, stateService)