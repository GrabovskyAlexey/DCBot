package ru.grabovsky.dungeoncrusherbot.strategy.message

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.VerificationErrorDto

@Component
class VerificationErrorMessage(messageGenerateService: MessageGenerateService) :
    AbstractSendMessage<VerificationErrorDto>(messageGenerateService) {
}