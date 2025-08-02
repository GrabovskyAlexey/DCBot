package ru.grabovsky.dungeoncrusherbot.strategy.message.maze

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.DataModel
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage

@Component
class SameCenterMessage(messageGenerateService: MessageGenerateService): AbstractSendMessage<DataModel>(messageGenerateService)