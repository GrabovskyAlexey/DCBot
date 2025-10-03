package ru.grabovsky.dungeoncrusherbot.strategy.dto

data class ExchangeDetailDto(
    val username: String?,
    val serverId: Int,
    val serverName: String?,
    val exchange: String?
) : DataModel
