package ru.grabovsky.dungeoncrusherbot.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "exchange_audit", schema = "dc_bot")
data class ExchangeAudit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    val eventType: ExchangeAuditEventType,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "request_type", length = 32)
    val requestType: String? = null,

    @Column(name = "source_server_id")
    val sourceServerId: Int? = null,

    @Column(name = "target_server_id")
    val targetServerId: Int? = null,

    @Column(name = "matches_count")
    val matchesCount: Int? = null,

    @Column(name = "contact_user_id")
    val contactUserId: Long? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false)
    val metadata: Map<String, Any?> = emptyMap(),

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)

enum class ExchangeAuditEventType {
    SEARCH_PERFORMED,
    CONTACTS_SHARED,
}
