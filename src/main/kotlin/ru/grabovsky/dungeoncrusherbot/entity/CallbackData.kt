package ru.grabovsky.dungeoncrusherbot.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant


@Entity
@Table(name = "callback_data", schema = "dc_bot")
data class CallbackData(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: CallbackDataType,
    @Column(name = "data")
    var data: String? = null
)

enum class CallbackDataType {
    RESOURCES, EXCHANGE
}

data class CallbackExchangeRequest(
    var type: ExchangeRequestType,
    val sourceServerId: Int,
    var targetServerId: Int? = null,
    var sourceResourceType: ExchangeResourceType,
    var targetResourceType: ExchangeResourceType,
    var sourceResourcePrice: Int? = null,
    var targetResourcePrice: Int? = null,
)