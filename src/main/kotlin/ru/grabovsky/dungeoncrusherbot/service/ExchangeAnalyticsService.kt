package ru.grabovsky.dungeoncrusherbot.service

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeAudit
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeAuditEventType
import ru.grabovsky.dungeoncrusherbot.repository.ExchangeAuditRepository
import ru.grabovsky.dungeoncrusherbot.service.events.ExchangeContactsSharedEvent
import ru.grabovsky.dungeoncrusherbot.service.events.ExchangeSearchPerformedEvent

@Service
class ExchangeAnalyticsService(
    private val exchangeAuditRepository: ExchangeAuditRepository,
    private val meterRegistry: MeterRegistry,
) {

    @EventListener
    @Transactional
    fun onSearchPerformed(event: ExchangeSearchPerformedEvent) {
        exchangeAuditRepository.save(
            ExchangeAudit(
                eventType = ExchangeAuditEventType.SEARCH_PERFORMED,
                userId = event.userId,
                sourceServerId = event.serverId,
                matchesCount = event.matchesCount,
                metadata = buildMap {
                    put("direction", event.direction.name)
                    if (event.matchTypeCounts.isNotEmpty()) {
                        put("matchTypeCounts", event.matchTypeCounts.mapKeys { it.key.name })
                    }
                }
            )
        )
        meterRegistry.counter(
            SEARCH_COUNTER_NAME,
            "server", event.serverId.toString(),
            "direction", event.direction.name,
        ).increment()
        meterRegistry.counter(
            "$SEARCH_COUNTER_NAME.matches",
            "server", event.serverId.toString(),
            "direction", event.direction.name,
        ).increment(event.matchesCount.toDouble())
    }

    @EventListener
    @Transactional
    fun onContactsShared(event: ExchangeContactsSharedEvent) {
        exchangeAuditRepository.save(
            ExchangeAudit(
                eventType = ExchangeAuditEventType.CONTACTS_SHARED,
                userId = event.userId,
                requestType = event.requestType.name,
                sourceServerId = event.sourceServerId,
                targetServerId = event.targetServerId,
                contactUserId = event.contactUserId,
                metadata = mapOf("requestId" to event.requestId),
            )
        )
        meterRegistry.counter(
            CONTACTS_COUNTER_NAME,
            "sourceServer", event.sourceServerId.toString(),
            "requestType", event.requestType.name,
        ).increment()
    }

    companion object {
        private const val SEARCH_COUNTER_NAME = "exchange.search.performed"
        private const val CONTACTS_COUNTER_NAME = "exchange.contacts.shared"
    }
}
