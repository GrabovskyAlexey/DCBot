package ru.grabovsky.dungeoncrusherbot.service.interfaces

sealed class ResourceOperation {
    data class Adjust(val type: AdjustType, val amount: Int) : ResourceOperation()
    data class SetExchange(val value: String) : ResourceOperation()
    object ClearExchange : ResourceOperation()
    data class SetExchangeUsername(val value: String) : ResourceOperation()
    object ClearExchangeUsername : ResourceOperation()
    object ToggleNotify : ResourceOperation()
    object MarkMain : ResourceOperation()
    object UnmarkMain : ResourceOperation()
}
