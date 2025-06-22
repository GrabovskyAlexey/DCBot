package ru.grabovsky.dungeoncrusherbot.strategy.state

import ru.grabovsky.dungeoncrusherbot.strategy.state.StateAction.*

enum class StateCode(val action: StateAction, val pause: Boolean = true, val template: String? = null, val markType: MarkType = MarkType.NONE) {
    START(SEND_MESSAGE, false),
    SUBSCRIBE(SEND_MESSAGE, markType = MarkType.UPDATE),
    UPDATE_SUBSCRIBE(UPDATE_MESSAGE, markType = MarkType.UPDATE),
    MAZE(SEND_MESSAGE, markType = MarkType.UPDATE),
    UPDATE_MAZE(UPDATE_MESSAGE, markType = MarkType.UPDATE),
    REFRESH_MAZE(SEND_MESSAGE, false),
    HELP(SEND_MESSAGE, false),
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