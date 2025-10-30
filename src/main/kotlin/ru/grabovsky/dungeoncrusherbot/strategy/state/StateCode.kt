package ru.grabovsky.dungeoncrusherbot.strategy.state

import ru.grabovsky.dungeoncrusherbot.strategy.state.MarkType.*
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateAction.*

enum class StateCode(
    val action: StateAction,
    val pause: Boolean = true,
    val template: String? = null,
    val markType: MarkType = NONE,
    val linkedStateCode: StateCode? = null,
) {
    WAITING(NOTHING),
    NOTIFICATION_SIEGE(NOTHING),
    NOTIFICATION_MINE(NOTHING),
    RELEASE_NOTES(NOTHING),
    NOTIFY(NOTHING),
    ADMIN_MESSAGE(NOTHING),
}

enum class StateAction {
    SEND_MESSAGE,
    UPDATE_MESSAGE,
    DELETE_MESSAGES,
    NOTHING,
}

enum class MarkType {
    DELETE,
    UPDATE,
    NONE,
}
