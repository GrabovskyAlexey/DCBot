package ru.grabovsky.dungeoncrusherbot.strategy.message

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService

@Component
class UpdateMazeMessage(messageGenerateService: MessageGenerateService, ) : MazeMessage(messageGenerateService)