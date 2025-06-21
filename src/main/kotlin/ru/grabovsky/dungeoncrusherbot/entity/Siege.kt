package ru.grabovsky.dungeoncrusherbot.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
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

//    @ManyToMany(mappedBy = "sieges")
//    val servers: MutableSet<Server> = mutableSetOf()
)