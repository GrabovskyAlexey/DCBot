package ru.grabovsky.dungeoncrusherbot.strategy.flow.resources

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import ru.grabovsky.dungeoncrusherbot.strategy.flow.resources.ResourcesPendingAction.*

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type")
@JsonSubTypes(
    Type(Amount::class, name = "amount"),
    Type(Exchange::class, name = "exchange"),
    Type(AddNote::class, name = "add_note"),
    Type(RemoveNote::class, name = "remove_note"),
    Type(ExchangeUsername::class, name = "exchange_username"),
)
sealed class ResourcesPendingAction {
    data class Amount(
        @param:JsonProperty("operation")
        @field:JsonProperty("operation")
        @param:JsonAlias("type")
        @field:JsonAlias("type")
        val operation: AmountActionType,
        val serverId: Int
    ) : ResourcesPendingAction()
    data class Exchange(val serverId: Int) : ResourcesPendingAction()
    data class AddNote(val serverId: Int) : ResourcesPendingAction()
    data class RemoveNote(val serverId: Int) : ResourcesPendingAction()
    data class ExchangeUsername(val serverId: Int) : ResourcesPendingAction()
}
