package ru.grabovsky.dungeoncrusherbot.service.events

import ru.grabovsky.dungeoncrusherbot.entity.ExchangeDirectionType
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequestType
import java.time.Instant

data class ExchangeSearchPerformedEvent(
    val userId: Long,
    val serverId: Int,
    val direction: ExchangeDirectionType,
    val matchesCount: Int,
    val matchTypeCounts: Map<ExchangeRequestType, Int>,
    val occurredAt: Instant = Instant.now(),
)

data class ExchangeContactsSharedEvent(
    val userId: Long,
    val requestId: Long,
    val contactUserId: Long,
    val requestType: ExchangeRequestType,
    val sourceServerId: Int,
    val targetServerId: Int?,
    val occurredAt: Instant = Instant.now(),
)
