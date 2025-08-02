package ru.grabovsky.dungeoncrusherbot.strategy.message.resources

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.DataModel
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage

@Component
class RemoveCbMessage(messageGenerateService: MessageGenerateService): AbstractSendMessage<DataModel>(messageGenerateService)