package ru.grabovsky.dungeoncrusherbot.strategy.dto

import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.entity.Location
import ru.grabovsky.dungeoncrusherbot.entity.Step

data class MazeDto(
    val location: Location = Location(0,0, Direction.CENTER),
    val steps: List<String>? = null
): DataModel {
    fun isComplete() = location.level >= 500
}