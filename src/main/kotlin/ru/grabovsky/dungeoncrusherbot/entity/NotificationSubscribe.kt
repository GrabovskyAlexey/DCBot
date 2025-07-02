package ru.grabovsky.dungeoncrusherbot.entity

import jakarta.persistence.*

@Entity
@Table(name = "notification_subscribe", schema = "dc_bot")
data class NotificationSubscribe(
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User,
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    val type: NotificationType,
    @Column(name = "enabled")
    var enabled: Boolean = true
)

enum class NotificationType {
    SIEGE, MINE
}
