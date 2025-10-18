package ru.grabovsky.dungeoncrusherbot.strategy.dto

import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequestType

data class ExchangeDetailDto(
    val username: String?,
    val serverId: Int,
    val requests: List<ExchangeRequestDto>,
) : DataModel
