package ru.grabovsky.dungeoncrusherbot.strategy.flow.maze

import ru.grabovsky.dungeoncrusherbot.entity.Direction

data class MazeFlowState(
    var pendingDirection: Direction? = null,
    val promptBindings: MutableList<String> = mutableListOf(),
)
