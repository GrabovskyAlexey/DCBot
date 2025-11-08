package ru.grabovsky.dungeoncrusherbot.strategy.flow.maze

import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStep

enum class MazeFlowStep(override val key: String) : FlowStep {
    MAIN("main"),
    CONFIRM_RESET("confirm_reset"),
    PROMPT("prompt"),
}
