package ru.grabovsky.dungeoncrusherbot.strategy.dto


data class AdminMessageDto(
    val firstName: String,
    val userName: String?,
    val userId: Long,
    val text: String
): DataModel