package ru.grabovsky.dungeoncrusherbot.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "maze", schema = "dc_bot")
data class Maze(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @OneToOne
    @JoinColumn(name = "user_id")
    val user: User,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "current_location")
    var currentLocation: Location? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "steps")
    val steps: MutableList<Step> = mutableListOf()
)

data class Step(
    val direction: Direction,
    val startLocation: Location,
    val finishLocation: Location
) {
    override fun toString(): String {
        return when(direction) {
            Direction.LEFT -> "$startLocation ↖\uFE0F $finishLocation"
            Direction.CENTER -> "$startLocation ⬆\uFE0F $finishLocation"
            Direction.RIGHT -> "$startLocation ↗\uFE0F $finishLocation"
        }
    }

}

data class Location(
    val level: Int,
    val offset: Int,
    val direction: Direction
) {
    override fun toString(): String {
        return when(direction) {
            Direction.CENTER -> "Этаж $level 0"
            Direction.LEFT -> "Этаж $level L$offset"
            Direction.RIGHT -> "Этаж $level R$offset"
        }
    }
}

enum class Direction {
    LEFT, CENTER, RIGHT
}