package ru.grabovsky.dungeoncrusherbot.strategy.flow.resources

data class ResourcesFlowState(
    var selectedServerId: Int? = null,
    var showHistory: Boolean = false,
    var resourcesPendingAction: ResourcesPendingAction? = null,
    val promptBindings: MutableList<String> = mutableListOf(),
)