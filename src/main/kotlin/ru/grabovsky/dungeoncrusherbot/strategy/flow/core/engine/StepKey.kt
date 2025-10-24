package ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine


enum class StepKey(override val key: String) : FlowStep {
    // Общий
    MAIN("main"),
    // Ресурсы
    SERVER("server"),
    PROMPT_AMOUNT("prompt_amount"),
    PROMPT_TEXT("prompt_text"),
    // Настройки
    SEND_REPORT("send_report")
}