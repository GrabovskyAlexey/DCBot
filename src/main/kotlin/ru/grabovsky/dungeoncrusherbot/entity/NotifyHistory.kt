package ru.grabovsky.dungeoncrusherbot.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "notify_history", schema = "dc_bot")
data class NotifyHistory (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null,
    @Column(name = "user_id", nullable = false)
    var userId: Long? = null,
    @Column(name = "message_id", nullable = false)
    var messageId: Int,
    @Column(name = "text", length = Integer.MAX_VALUE)
    var text: String,
    @Column(name = "send_time")
    var sendTime: Instant,
    @Column(name = "deleted")
    var deleted: Boolean = false,
)