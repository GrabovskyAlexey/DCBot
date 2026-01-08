package ru.grabovsky.dungeoncrusherbot.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "resource_server_history", schema = "dc_bot")
data class ResourceServerHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_state_id", nullable = false)
    val serverState: ResourceServerState,

    @Column(name = "event_date", nullable = false)
    val eventDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "resource", nullable = false)
    val resource: ResourceType,

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    val direction: DirectionType,

    @Column(name = "quantity", nullable = false)
    val quantity: Int,

    @Column(name = "from_server")
    val fromServer: Int? = null,

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

    @Column(name = "prev_draador_count")
    val prevDraadorCount: Int? = null,

    @Column(name = "prev_void_count")
    val prevVoidCount: Int? = null,

    @Column(name = "prev_cb_count")
    val prevCbCount: Int? = null,

    @Column(name = "prev_balance")
    val prevBalance: Int? = null,
)
