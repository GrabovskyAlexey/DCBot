package ru.grabovsky.dungeoncrusherbot.entity

import jakarta.persistence.*
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity
@Table(name = "resource_server_state", schema = "dc_bot")
data class ResourceServerState(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    val server: Server,

    @Column(name = "exchange_label")
    var exchangeLabel: String? = null,

    @Column(name = "exchange_username")
    var exchangeUsername: String? = null,

    @Column(name = "exchange_user_id")
    var exchangeUserId: Long? = null,

    @Column(name = "draador_count", nullable = false)
    var draadorCount: Int = 0,

    @Column(name = "void_count", nullable = false)
    var voidCount: Int = 0,

    @Column(name = "cb_count", nullable = false)
    var cbCount: Int = 0,

    @Column(name = "balance", nullable = false)
    var balance: Int = 0,

    @Column(name = "notify_disable", nullable = false)
    var notifyDisable: Boolean = false,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
