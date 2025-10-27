package ru.grabovsky.dungeoncrusherbot.strategy.flow.maze

enum class MazeFlowStep(val key: String) {
    MAIN("main"),
    CONFIRM_RESET("confirm_reset"),
    PROMPT("prompt"),
}
