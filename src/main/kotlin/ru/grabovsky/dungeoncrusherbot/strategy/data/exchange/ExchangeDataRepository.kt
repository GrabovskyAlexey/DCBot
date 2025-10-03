package ru.grabovsky.dungeoncrusherbot.strategy.data.exchange

import org.springframework.stereotype.Repository
import org.telegram.telegrambots.meta.api.objects.User as TgUser
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.data.AbstractDataRepository
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeDto

@Repository
open class ExchangeDataRepository(
    protected val userService: UserService,
    protected val serverService: ServerService,
    protected val stateService: StateService,
) : AbstractDataRepository<ExchangeDto>() {

    override fun getData(user: TgUser): ExchangeDto {
        val entityUser = userService.getUser(user.id)
            ?: throw IllegalStateException("User with id: ${user.id} not found")
        val resources = entityUser.resources

        val servers = serverService.getAllServers()
            .sortedBy { it.id }
            .map { server ->
                val exchange = resources?.data?.servers?.get(server.id)?.exchange?.takeIf { it.isNotBlank() }
                ExchangeDto.Server(
                    id = server.id,
                    name = server.name,
                    hasExchange = exchange != null,
                    exchange = exchange,
                    main = resources?.data?.mainServerId == server.id
                )
            }

        return ExchangeDto(
            username = entityUser.userName ?: entityUser.firstName,
            servers = servers
        )
    }
}
