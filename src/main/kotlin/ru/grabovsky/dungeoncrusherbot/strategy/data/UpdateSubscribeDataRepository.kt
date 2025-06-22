package ru.grabovsky.dungeoncrusherbot.strategy.data

import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Repository
class UpdateSubscribeDataRepository(userService: UserService) : SubscribeDataRepository(userService)