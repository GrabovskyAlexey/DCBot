package ru.grabovsky.dungeoncrusherbot.strategy.data.exchange

import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Repository
class UpdateExchangeDataRepository(
    userService: UserService,
    serverService: ServerService,
    stateService: StateService,
) : ExchangeDataRepository(userService, serverService, stateService)
