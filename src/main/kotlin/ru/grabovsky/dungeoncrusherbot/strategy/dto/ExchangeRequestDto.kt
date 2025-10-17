package ru.grabovsky.dungeoncrusherbot.strategy.dto

import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequestType

data class ExchangeRequestDto(
    val pos: Int,
    val id: Long,
    val type: ExchangeRequestType,
    val sourcePrice: Int,
    val targetPrice: Int,
    val targetServerId: Int?,
    val sourceServerId: Int
)