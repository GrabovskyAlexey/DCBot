package ru.grabovsky.dungeoncrusherbot.util

import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.DeleteMessageAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowAction


object FlowUtils {
    fun cleanupPromptActions(bindings: List<String>): MutableList<FlowAction> =
        bindings.fold(mutableListOf()) { acc, binding ->
            acc += DeleteMessageAction(binding)
            acc
        }
}