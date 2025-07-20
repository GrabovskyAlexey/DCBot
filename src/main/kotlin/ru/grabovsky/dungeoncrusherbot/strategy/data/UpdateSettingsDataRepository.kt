package ru.grabovsky.dungeoncrusherbot.strategy.data

import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Repository
class UpdateSettingsDataRepository(userService: UserService): SettingsDataRepository(userService)