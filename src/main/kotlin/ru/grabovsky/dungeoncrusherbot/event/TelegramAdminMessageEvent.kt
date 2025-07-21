package ru.grabovsky.dungeoncrusherbot.event

import ru.grabovsky.dungeoncrusherbot.strategy.dto.AdminMessageDto

data class TelegramAdminMessageEvent(
    val chatId: Long,
    val dto: AdminMessageDto
)
