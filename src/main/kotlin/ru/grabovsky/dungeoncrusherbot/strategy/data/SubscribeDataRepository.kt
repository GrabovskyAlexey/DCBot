package ru.grabovsky.dungeoncrusherbot.strategy.data

import org.springframework.stereotype.Repository
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerDto

@Repository
class SubscribeDataRepository(
    private val userService: UserService
) : AbstractDataRepository<ServerDto>() {
    override fun getData(
        user: User
    ): ServerDto {
        val user = userService.getUser(user.id)
        val servers = user?.servers?.map { it.id }?.sorted() ?: emptyList()
        return  ServerDto(servers)
    }
}