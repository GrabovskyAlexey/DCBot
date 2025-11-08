package ru.grabovsky.dungeoncrusherbot.strategy.flow.help

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.AbstractStaticFlow
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStep

@Component
class HelpFlow : AbstractStaticFlow(
    key = FlowKeys.HELP,
    step = StepKey.MAIN,
    bindingKey = MAIN_MESSAGE_BINDING,
    modelProvider = { null }
) {
    companion object {
        private const val MAIN_MESSAGE_BINDING = "help_main"
    }
}

enum class StepKey(override val key: String) : FlowStep {
    MAIN("main"),
}
