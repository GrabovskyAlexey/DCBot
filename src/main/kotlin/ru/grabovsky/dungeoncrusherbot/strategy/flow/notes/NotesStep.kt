package ru.grabovsky.dungeoncrusherbot.strategy.flow.notes

import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStep

enum class NotesStep(override val key: String) : FlowStep {
    MAIN("main"),
    PROMPT_TEXT("prompt_text"),
}
