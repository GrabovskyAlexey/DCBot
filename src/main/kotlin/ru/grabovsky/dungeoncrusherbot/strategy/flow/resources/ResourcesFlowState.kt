package ru.grabovsky.dungeoncrusherbot.strategy.flow.resources

import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.PromptState

data class ResourcesFlowState(
    var selectedServerId: Int? = null,
    var showHistory: Boolean = false,
    var resourcesPendingAction: ResourcesPendingAction? = null,
    override val promptBindings: MutableList<String> = mutableListOf(),
) : PromptState
