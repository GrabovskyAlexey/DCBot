package ru.grabovsky.dungeoncrusherbot.strategy.state

import ru.grabovsky.dungeoncrusherbot.strategy.state.StateAction.*

enum class StateCode(val action: StateAction, val pause: Boolean = true, val template: String? = null, val markType: MarkType = MarkType.NONE) {
    START(SEND_MESSAGE, false),
    SUBSCRIBE(SEND_MESSAGE, markType = MarkType.UPDATE),
    UPDATE_SUBSCRIBE(UPDATE_MESSAGE, markType = MarkType.UPDATE),
    MAZE(SEND_MESSAGE, markType = MarkType.UPDATE),
    UPDATE_MAZE(UPDATE_MESSAGE, markType = MarkType.UPDATE),
    CONFIRM_REFRESH_MAZE(UPDATE_MESSAGE),
    HELP(SEND_MESSAGE, false),
    NOTIFY(SEND_MESSAGE, markType = MarkType.UPDATE),
    UPDATE_NOTIFY(UPDATE_MESSAGE, markType = MarkType.UPDATE),
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