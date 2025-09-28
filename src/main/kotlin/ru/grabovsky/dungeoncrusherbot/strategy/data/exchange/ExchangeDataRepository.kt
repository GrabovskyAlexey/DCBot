package ru.grabovsky.dungeoncrusherbot.strategy.data.exchange

import org.springframework.stereotype.Repository
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.data.AbstractDataRepository
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeDto

@Repository
class ExchangeDataRepository(
    private val userService: UserService
) : AbstractDataRepository<ExchangeDto>() {
    override fun getData(user: User): ExchangeDto {
        val entityUser = userService.getUser(user.id)
        val username = entityUser?.userName
            ?: entityUser?.firstName
            ?: user.userName
            ?: user.firstName
        return ExchangeDto(username)
    }
}
