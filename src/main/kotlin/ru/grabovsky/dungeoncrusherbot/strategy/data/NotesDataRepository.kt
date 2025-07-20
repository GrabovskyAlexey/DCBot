package ru.grabovsky.dungeoncrusherbot.strategy.data

import org.springframework.stereotype.Repository
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.NotesDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ResourceDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerResourceDto

@Repository
class NotesDataRepository(
    private val userService: UserService
) : AbstractDataRepository<NotesDto>() {
    override fun getData(user: User): NotesDto {
        val notes = userService.getUser(user.id)?.notes ?: emptyList()
        return NotesDto(notes)
    }
}