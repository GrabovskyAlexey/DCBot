package ru.grabovsky.dungeoncrusherbot.strategy.data.resources

import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Repository
class UpdateResourcesDataRepository(userService: UserService): ResourcesDataRepository(userService)