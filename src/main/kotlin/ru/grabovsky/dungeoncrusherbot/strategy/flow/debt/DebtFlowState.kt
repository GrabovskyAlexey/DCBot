package ru.grabovsky.dungeoncrusherbot.strategy.flow.debt

import ru.grabovsky.dungeoncrusherbot.entity.DebtDirection
import ru.grabovsky.dungeoncrusherbot.entity.DebtResourceType
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStep
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.PromptState

data class DebtFlowState(
    var creation: DebtCreationState? = null,
    var editDebtId: Long? = null,
    override val promptBindings: MutableList<String> = mutableListOf(),
) : PromptState

data class DebtCreationState(
    var phase: DebtCreationPhase = DebtCreationPhase.DIRECTION,
    var direction: DebtDirection? = null,
    var serverId: Int? = null,
    var resourceType: DebtResourceType? = null,
    var amount: Int? = null,
    var counterpartyName: String? = null,
)

enum class DebtCreationPhase {
    DIRECTION,
    SERVER,
    RESOURCE,
    AMOUNT,
    NAME,
}

enum class DebtStep(override val key: String) : FlowStep {
    MAIN("main"),
    CREATE("create"),
    PROMPT_AMOUNT("prompt_amount"),
    PROMPT_NAME("prompt_name"),
}
