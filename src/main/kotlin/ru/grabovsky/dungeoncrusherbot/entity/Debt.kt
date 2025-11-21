package ru.grabovsky.dungeoncrusherbot.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity
@Table(name = "debts", schema = "dc_bot")
data class Debt(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    var direction: DebtDirection,

    @Column(name = "server_id", nullable = false)
    var serverId: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    var resourceType: DebtResourceType,

    @Column(name = "amount", nullable = false)
    var amount: Int,

    @Column(name = "counterparty_name", nullable = false)
    var counterpartyName: String,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant? = null,
)

enum class DebtDirection {
    OWE_ME,
    I_OWE,
}

enum class DebtResourceType {
    VOID,
    MAP,
    CB,
    BOTTLES,
    CANNON,
}
