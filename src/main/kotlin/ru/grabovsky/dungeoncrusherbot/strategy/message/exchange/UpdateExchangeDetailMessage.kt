package ru.grabovsky.dungeoncrusherbot.strategy.message.exchange

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService

@Component
class UpdateExchangeDetailMessage(
    messageGenerateService: MessageGenerateService
) : ExchangeDetailMessage(messageGenerateService)
