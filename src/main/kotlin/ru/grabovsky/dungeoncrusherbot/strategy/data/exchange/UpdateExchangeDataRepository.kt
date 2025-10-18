package ru.grabovsky.dungeoncrusherbot.strategy.data.exchange

import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ExchangeRequestService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Repository
class UpdateExchangeDataRepository(
    userService: UserService,
    serverService: ServerService,
    exchangeRequestService: ExchangeRequestService
) : ExchangeDataRepository(userService, serverService, exchangeRequestService)
