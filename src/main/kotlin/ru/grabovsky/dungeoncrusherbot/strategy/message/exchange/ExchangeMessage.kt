package ru.grabovsky.dungeoncrusherbot.strategy.message.exchange

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ExchangeDto
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage

@Component
open class ExchangeMessage(
    messageGenerateService: MessageGenerateService
) : AbstractSendMessage<ExchangeDto>(messageGenerateService)
