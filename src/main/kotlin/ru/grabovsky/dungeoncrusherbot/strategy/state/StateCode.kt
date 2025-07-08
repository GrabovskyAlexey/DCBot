package ru.grabovsky.dungeoncrusherbot.strategy.state

import ru.grabovsky.dungeoncrusherbot.strategy.state.StateAction.*

enum class StateCode(val action: StateAction, val pause: Boolean = true, val template: String? = null, val markType: MarkType = MarkType.NONE, val linkedStateCode: StateCode? = null) {
    START(SEND_MESSAGE, false),
    SUBSCRIBE(SEND_MESSAGE, markType = MarkType.UPDATE),
    UPDATE_SUBSCRIBE(UPDATE_MESSAGE, linkedStateCode = SUBSCRIBE),
    MAZE(SEND_MESSAGE, markType = MarkType.UPDATE),
    UPDATE_MAZE(UPDATE_MESSAGE, linkedStateCode = MAZE),
    CONFIRM_REFRESH_MAZE(UPDATE_MESSAGE, linkedStateCode = MAZE),
    HELP(SEND_MESSAGE, false),
    NOTIFY(SEND_MESSAGE, markType = MarkType.UPDATE),
    UPDATE_NOTIFY(UPDATE_MESSAGE, linkedStateCode = NOTIFY),
    WAITING(NOTHING),
    NOTIFICATION_SIEGE(NOTHING),
    NOTIFICATION_MINE(NOTHING),
    RELEASE_NOTES(NOTHING),
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