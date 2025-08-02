package ru.grabovsky.dungeoncrusherbot.strategy.data

import org.springframework.stereotype.Repository
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.strategy.dto.StartDto

@Repository
class StartDataRepository: AbstractDataRepository<StartDto>() {
    override fun getData(
        user: User
    ) = StartDto(username = user.userName?:user.firstName)
}