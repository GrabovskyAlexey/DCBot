package ru.grabovsky.dungeoncrusherbot.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity
@Table(name = "exchange_requests", schema = "dc_bot")
data class ExchangeRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: ExchangeRequestType,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "source_server_id", nullable = false)
    val sourceServerId: Int,

    @Column(name = "target_server_id")
    var targetServerId: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "source_resource_type", nullable = false)
    var sourceResourceType: ExchangeResourceType,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_resource_type", nullable = false)
    var targetResourceType: ExchangeResourceType,

    @Column(name = "source_resource_price", nullable = false)
    var sourceResourcePrice: Int,

    @Column(name = "target_resource_price", nullable = false)
    var targetResourcePrice: Int,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = false,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

enum class ExchangeRequestType {
    EXCHANGE_MAP,
    EXCHANGE_VOID,
    SELL_MAP,
    BUY_MAP,
}

enum class ExchangeResourceType {
    MAP,
    VOID,
}
