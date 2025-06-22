package ru.grabovsky.dungeoncrusherbot.entity

import jakarta.persistence.*
import java.time.LocalTime

@Entity
@Table(name = "sieges", schema = "dc_bot")
data class Siege(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Int,

    @Column(name = "siege_time", nullable = false)
    val siegeTime: LocalTime,
)