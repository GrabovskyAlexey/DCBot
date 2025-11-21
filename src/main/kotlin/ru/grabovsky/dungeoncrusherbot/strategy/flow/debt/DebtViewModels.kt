package ru.grabovsky.dungeoncrusherbot.strategy.flow.debt

data class DebtOverviewModel(
    val oweMe: List<DebtItemModel>,
    val iOwe: List<DebtItemModel>,
)

data class DebtItemModel(
    val id: Long,
    val displayNumber: Int,
    val directionLabel: String,
    val resourceLabel: String,
    val amount: Int,
    val server: String,
    val counterparty: String,
)

data class DebtCreationViewModel(
    val phase: DebtCreationPhase,
    val direction: String?,
    val server: String?,
    val resource: String?,
    val amount: Int?,
    val counterparty: String?,
)

data class DebtPromptModel(
    val title: String,
    val invalid: Boolean,
    val creation: DebtCreationViewModel,
)
