package ru.grabovsky.dungeoncrusherbot.strategy.flow.admin

import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStep

enum class AdminMessageStep(override val key: String) : FlowStep {
    MAIN("main"),
    PROMPT_REPLY("prompt_reply"),
    INFO("info"),
}
