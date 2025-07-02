package ru.grabovsky.dungeoncrusherbot.entity

import jakarta.persistence.*

@Entity
@Table(name = "update_messages", schema = "dc_bot")
data class UpdateMessage(
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "version")
    val version: String,
    @Column(name = "text")
    val text: String,
    @Column(name = "sent")
    var sent: Boolean = true
)

