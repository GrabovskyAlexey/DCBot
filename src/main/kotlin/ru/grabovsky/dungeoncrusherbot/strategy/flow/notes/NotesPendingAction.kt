package ru.grabovsky.dungeoncrusherbot.strategy.flow.notes

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonSubTypes.Type

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type")
@JsonSubTypes(
    Type(NotesPendingAction.Add::class, name = "add"),
    Type(NotesPendingAction.Remove::class, name = "remove"),
)
sealed class NotesPendingAction {
    data object Add : NotesPendingAction()
    data object Remove : NotesPendingAction()
}
