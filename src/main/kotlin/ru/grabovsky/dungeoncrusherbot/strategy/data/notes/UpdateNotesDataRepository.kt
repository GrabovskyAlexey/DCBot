package ru.grabovsky.dungeoncrusherbot.strategy.data.notes

import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Repository
class UpdateNotesDataRepository(userService: UserService): NotesDataRepository(userService)