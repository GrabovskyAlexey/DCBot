package ru.grabovsky.dungeoncrusherbot.entity

import jakarta.persistence.*
import java.time.LocalTime

@Entity
@Table(name = "admin_messages", schema = "dc_bot")
data class AdminMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "message", nullable = false)
    val message: String
)