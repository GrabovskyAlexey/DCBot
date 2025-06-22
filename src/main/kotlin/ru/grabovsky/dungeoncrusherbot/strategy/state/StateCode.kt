package ru.grabovsky.dungeoncrusherbot.strategy.state

import ru.grabovsky.dungeoncrusherbot.strategy.state.StateAction.*

enum class StateCode(val action: StateAction, val pause: Boolean = true, val template: String? = null, val markType: MarkType = MarkType.NONE) {
    START(SEND_MESSAGE),
    SUBSCRIBE(SEND_MESSAGE, markType = MarkType.UPDATE),
    UPDATE_SUBSCRIBE(UPDATE_MESSAGE, markType = MarkType.UPDATE),
    WAITING(NOTHING),
    NOTIFICATION(NOTHING)
}

enum class StateAction {
    SEND_MESSAGE,
    UPDATE_MESSAGE,
    DELETE_MESSAGES,
    NOTHING
}

enum class MarkType{
    DELETE, UPDATE, NONE
}