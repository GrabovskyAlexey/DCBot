package ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support

import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowInlineButton
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKey
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowMessage
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowParseMode
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowReplyButton
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStep

fun FlowKey.buildMessage(
    step: FlowStep,
    model: Any? = null,
    inlineButtons: List<FlowInlineButton> = emptyList(),
    replyButtons: List<FlowReplyButton> = emptyList(),
    parseMode: FlowParseMode = FlowParseMode.MARKDOWN,
): FlowMessage = FlowMessage(
    flowKey = this,
    stepKey = step.key,
    model = model,
    inlineButtons = inlineButtons,
    replyButtons = replyButtons,
    parseMode = parseMode
)
