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
import ru.grabovsky.dungeoncrusherbot.service.events.GlobalExchangeSearchPerformedEvent

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

    @EventListener
    @Transactional
    fun onGlobalSearchPerformed(event: GlobalExchangeSearchPerformedEvent) {
        exchangeAuditRepository.save(
            ExchangeAudit(
                eventType = ExchangeAuditEventType.GLOBAL_SEARCH_PERFORMED,
                userId = event.userId,
                matchesCount = event.totalMatchesCount,
                metadata = buildMap {
                    put("userRequestsCount", event.userRequestsCount)
                    put("totalMatchesCount", event.totalMatchesCount)
                    if (event.matchesPerRequest.isNotEmpty()) {
                        put("matchesPerRequest", event.matchesPerRequest)
                    }
                }
            )
        )
        meterRegistry.counter(
            GLOBAL_SEARCH_COUNTER_NAME,
        ).increment()
        meterRegistry.counter(
            "$GLOBAL_SEARCH_COUNTER_NAME.matches",
        ).increment(event.totalMatchesCount.toDouble())
        meterRegistry.counter(
            "$GLOBAL_SEARCH_COUNTER_NAME.user_requests",
        ).increment(event.userRequestsCount.toDouble())
    }

    companion object {
        private const val SEARCH_COUNTER_NAME = "exchange.search.performed"
        private const val CONTACTS_COUNTER_NAME = "exchange.contacts.shared"
        private const val GLOBAL_SEARCH_COUNTER_NAME = "exchange.global_search.performed"
    }
}
