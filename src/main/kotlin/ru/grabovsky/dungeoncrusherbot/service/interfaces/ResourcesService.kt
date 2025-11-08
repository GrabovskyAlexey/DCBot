package ru.grabovsky.dungeoncrusherbot.service.interfaces

import org.telegram.telegrambots.meta.api.objects.User

interface ResourcesService {
    fun applyOperation(user: User, serverId: Int, operation: ResourceOperation)
}

sealed class ResourceOperation {
    data class Adjust(val type: AdjustType, val amount: Int) : ResourceOperation()
    data class SetExchange(val value: String) : ResourceOperation()
    object ClearExchange : ResourceOperation()
    object ToggleNotify : ResourceOperation()
    object MarkMain : ResourceOperation()
    object UnmarkMain : ResourceOperation()
}

enum class AdjustType {
    ADD_DRAADOR,
    SELL_DRAADOR,
    SEND_DRAADOR,
    RECEIVE_DRAADOR,
    ADD_VOID,
    REMOVE_VOID,
    ADD_CB,
    REMOVE_CB,
}