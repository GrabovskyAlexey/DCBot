package ru.grabovsky.dungeoncrusherbot.strategy.flow.exchange

import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStep

enum class ExchangeFlowStep(override val key: String) : FlowStep {
    MAIN("main"),
    DETAIL("detail"),
    TARGET_SERVER("target_server"),
    SOURCE_PRICE("source_price"),
    TARGET_PRICE("target_price"),
    REMOVE("remove"),
    SEARCH("search"),
    SEARCH_RESULT("search_result"),
}
