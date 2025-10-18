package ru.grabovsky.dungeoncrusherbot.strategy.data.exchange

import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeDirectionType
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ExchangeRequestService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.data.AbstractDataRepository
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeDetailDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeRequestDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Repository
class ExchangeDetailDataRepository(
    private val userService: UserService,
    private val stateService: StateService,
    private val exchangeRequestService: ExchangeRequestService,
) : AbstractDataRepository<ExchangeDetailDto>() {

    override fun getData(user: TgUser): ExchangeDetailDto {
        val entityUser = userService.getUser(user.id)
            ?: throw IllegalStateException("User with id: ${user.id} not found")
        val state = stateService.getState(user)
        val lastServerId = state.lastServerIdByState[StateCode.EXCHANGE]
            ?: throw IllegalStateException("Last server id for state: $state not found")

        var i = 1;
        val requests = exchangeRequestService.getActiveExchangeRequestsByServer(
            entityUser,
            lastServerId,
            ExchangeDirectionType.SOURCE
        )
            .filter { it.isActive }
            .map { request ->
                ExchangeRequestDto(
                    pos = i++,
                    id = request.id!!,
                    type = request.type,
                    sourcePrice = request.sourceResourcePrice,
                    targetPrice = request.targetResourcePrice,
                    targetServerId = request.targetServerId,
                    sourceServerId = request.sourceServerId,
                )
            }

        return ExchangeDetailDto(
            username = entityUser.userName ?: entityUser.firstName,
            serverId = lastServerId,
            requests = requests,
        )
    }
}