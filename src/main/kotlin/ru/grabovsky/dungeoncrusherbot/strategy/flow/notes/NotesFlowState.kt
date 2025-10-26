package ru.grabovsky.dungeoncrusherbot.strategy.flow.notes

data class NotesFlowState(
    var pendingAction: NotesPendingAction? = null,
    val promptBindings: MutableList<String> = mutableListOf(),
)
