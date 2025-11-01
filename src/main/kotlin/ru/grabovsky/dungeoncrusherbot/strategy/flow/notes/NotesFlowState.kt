package ru.grabovsky.dungeoncrusherbot.strategy.flow.notes

import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.PromptState

data class NotesFlowState(
    var pendingAction: NotesPendingAction? = null,
    override val promptBindings: MutableList<String> = mutableListOf(),
) : PromptState
