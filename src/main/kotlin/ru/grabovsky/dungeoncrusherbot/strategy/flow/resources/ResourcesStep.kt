package ru.grabovsky.dungeoncrusherbot.strategy.flow.resources

import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStep

enum class ResourcesStep(override val key: String) : FlowStep {
    MAIN("main"),
    SERVER("server"),
    PROMPT_AMOUNT("prompt_amount"),
    PROMPT_TEXT("prompt_text"),
}
