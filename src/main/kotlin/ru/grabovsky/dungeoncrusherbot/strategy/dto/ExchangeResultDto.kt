package ru.grabovsky.dungeoncrusherbot.strategy.dto


data class ExchangeResultDto(
    val username: String?,
    val firstName: String,
    val active: Boolean,
    val request: ExchangeRequestDto
) : DataModel
