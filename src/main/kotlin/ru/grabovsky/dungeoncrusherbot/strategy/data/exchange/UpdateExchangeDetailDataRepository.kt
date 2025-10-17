package ru.grabovsky.dungeoncrusherbot.strategy.data.exchange

import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ExchangeRequestService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Repository
class UpdateExchangeDetailDataRepository(
    userService: UserService,
    stateService: StateService,
    exchangeRequestService: ExchangeRequestService,
) : ExchangeDetailDataRepository(userService, stateService, exchangeRequestService)
