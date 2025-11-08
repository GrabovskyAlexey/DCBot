package ru.grabovsky.dungeoncrusherbot.strategy.flow.maze

import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.PromptState

data class MazeFlowState(
    var pendingDirection: Direction? = null,
    override val promptBindings: MutableList<String> = mutableListOf(),
) : PromptState
