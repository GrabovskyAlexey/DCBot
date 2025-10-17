package ru.grabovsky.dungeoncrusherbot.strategy.state.exchange

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService

@Component
class UpdateExchangeDetailState(stateService: StateService) : ExchangeDetailState(stateService)
