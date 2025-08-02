package ru.grabovsky.dungeoncrusherbot.event

import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.strategy.dto.AdminMessageDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

data class TelegramAdminMessageEvent(
    override val user: User,
    override val stateCode: StateCode,
    val adminChatId: Long,
    val dto: AdminMessageDto
) : TelegramEvent
