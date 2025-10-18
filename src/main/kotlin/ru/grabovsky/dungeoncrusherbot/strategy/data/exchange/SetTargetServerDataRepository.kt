package ru.grabovsky.dungeoncrusherbot.strategy.data.exchange

import org.springframework.stereotype.Repository
import org.telegram.telegrambots.meta.api.objects.User as TgUser
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.data.AbstractDataRepository
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

@Repository
class SetTargetServerDataRepository(
    private val userService: UserService,
    private val serverService: ServerService,
    private val stateService: StateService,
) : AbstractDataRepository<ExchangeDto>() {

    override fun getData(user: TgUser): ExchangeDto {
        val entityUser = userService.getUser(user.id)
            ?: throw IllegalStateException("User with id: ${user.id} not found")
        val resources = entityUser.resources

        val lastServerId = stateService.getState(user).lastServerIdByState[StateCode.EXCHANGE]

        val servers = serverService.getAllServers()
            .sortedBy { it.id }
            .filterNot { it.id == lastServerId }
            .map { server ->
                ExchangeDto.Server(
                    id = server.id,
                    main = resources?.data?.mainServerId == server.id,
                    hasRequests = false
                )
            }

        return ExchangeDto(
            servers = servers,
            username = user.userName
        )
    }
}
