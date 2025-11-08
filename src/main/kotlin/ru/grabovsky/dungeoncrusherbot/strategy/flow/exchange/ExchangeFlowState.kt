package ru.grabovsky.dungeoncrusherbot.strategy.flow.exchange

import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequestType
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeResourceType

data class ExchangeFlowState(
    val selectedServerId: Int? = null,
    val pendingRequest: PendingRequest? = null,
) {
    data class PendingRequest(
        val type: ExchangeRequestType,
        val sourceServerId: Int,
        val targetServerId: Int? = null,
        val sourceResourceType: ExchangeResourceType,
        val targetResourceType: ExchangeResourceType,
        val sourcePrice: Int? = null,
        val targetPrice: Int? = null,
    )
}
