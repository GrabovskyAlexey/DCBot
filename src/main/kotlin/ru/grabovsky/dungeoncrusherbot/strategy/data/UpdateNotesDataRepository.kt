package ru.grabovsky.dungeoncrusherbot.strategy.data

import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Repository
class UpdateNotesDataRepository(userService: UserService): NotesDataRepository(userService)