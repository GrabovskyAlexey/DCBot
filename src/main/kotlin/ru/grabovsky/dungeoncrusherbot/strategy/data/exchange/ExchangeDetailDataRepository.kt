package ru.grabovsky.dungeoncrusherbot.strategy.data.exchange

import org.springframework.stereotype.Repository
import org.telegram.telegrambots.meta.api.objects.User as TgUser
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.data.AbstractDataRepository
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeDetailDto

abstract class AbstractExchangeDetailDataRepository(
    private val userService: UserService,
    private val serverService: ServerService,
    private val stateService: StateService,
) : AbstractDataRepository<ExchangeDetailDto>() {

    override fun getData(user: TgUser): ExchangeDetailDto {
        val entityUser = userService.getUser(user.id)
            ?: throw IllegalStateException("User with id: ${user.id} not found")
        val state = stateService.getState(user)
        val selectedId = state.callbackData?.substringAfter("DETAIL:")?.toIntOrNull()
            ?: throw IllegalStateException("Selected server id not found for user ${user.id}")

        val resources = entityUser.resources
        val exchange = resources?.data?.servers?.get(selectedId)?.exchange?.takeIf { it.isNotBlank() }
        val server = serverService.getServerById(selectedId)

        return ExchangeDetailDto(
            username = entityUser.userName ?: entityUser.firstName,
            serverId = server.id,
            serverName = server.name,
            exchange = exchange
        )
    }
}

@Repository
class ExchangeDetailDataRepository(
    userService: UserService,
    serverService: ServerService,
    stateService: StateService,
) : AbstractExchangeDetailDataRepository(userService, serverService, stateService)

@Repository
class UpdateExchangeDetailDataRepository(
    userService: UserService,
    serverService: ServerService,
    stateService: StateService,
) : AbstractExchangeDetailDataRepository(userService, serverService, stateService)
