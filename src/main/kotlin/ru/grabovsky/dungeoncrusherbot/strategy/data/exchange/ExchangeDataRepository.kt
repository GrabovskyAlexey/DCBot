package ru.grabovsky.dungeoncrusherbot.strategy.data.exchange

import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ExchangeRequestService
import org.telegram.telegrambots.meta.api.objects.User as TgUser
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.data.AbstractDataRepository
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeDto

@Repository
class ExchangeDataRepository(
    private val userService: UserService,
    private val serverService: ServerService,
    private val exchangeRequestService: ExchangeRequestService,
) : AbstractDataRepository<ExchangeDto>() {

    override fun getData(user: TgUser): ExchangeDto {
        val entityUser = userService.getUser(user.id)
            ?: throw IllegalStateException("User with id: ${user.id} not found")
        val mainServerId = entityUser.profile?.mainServerId
        val existsRequestServers = exchangeRequestService.getActiveExchangeRequestsByUser(entityUser).map { it.sourceServerId }.toSet()

        val servers = serverService.getAllServers()
            .sortedBy { it.id }
            .map { server ->
                ExchangeDto.Server(
                    id = server.id,
                    main = mainServerId == server.id,
                    hasRequests = existsRequestServers.contains(server.id),
                )
            }

        return ExchangeDto(
            servers = servers,
            username = user.userName
        )
    }
}
