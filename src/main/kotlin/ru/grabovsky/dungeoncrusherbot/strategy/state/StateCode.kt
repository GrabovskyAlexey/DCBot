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
    MAZE(SEND_MESSAGE, markType = UPDATE),
    UPDATE_MAZE(UPDATE_MESSAGE, linkedStateCode = MAZE),
    CONFIRM_REFRESH_MAZE(UPDATE_MESSAGE, linkedStateCode = MAZE),
    EXCHANGE(SEND_MESSAGE, markType = UPDATE),
    UPDATE_EXCHANGE(UPDATE_MESSAGE, linkedStateCode = EXCHANGE),
    EXCHANGE_DETAIL(UPDATE_MESSAGE, linkedStateCode = EXCHANGE),
    UPDATE_EXCHANGE_DETAIL(UPDATE_MESSAGE, linkedStateCode = EXCHANGE),
    VERIFY(VERIFICATION, false),
    VERIFICATION_ERROR(SEND_MESSAGE, false, markType = DELETE),
    VERIFICATION_SUCCESS(DELETE_MESSAGES, false),
    ADD_EXCHANGE(SEND_MESSAGE, markType = DELETE),
    REMOVE_EXCHANGE(NOTHING, pause = false),
    SET_SOURCE_PRICE(UPDATE_MESSAGE, linkedStateCode = EXCHANGE),
    SET_TARGET_PRICE(UPDATE_MESSAGE, linkedStateCode = EXCHANGE),
    SET_TARGET_SERVER(UPDATE_MESSAGE, linkedStateCode = EXCHANGE),
    REMOVE_EXCHANGE_REQUEST(UPDATE_MESSAGE, linkedStateCode = EXCHANGE),
    SEARCH_EXCHANGE(UPDATE_MESSAGE, linkedStateCode = EXCHANGE),
    SEARCH_EXCHANGE_RESULT(UPDATE_MESSAGE, linkedStateCode = EXCHANGE),
    SEND_EXCHANGE_CONTACT(UPDATE_MESSAGE, linkedStateCode = EXCHANGE),
    SEND_REPORT(SEND_MESSAGE, markType = DELETE),
    SAME_LEFT(SEND_MESSAGE, markType = DELETE),
    SAME_RIGHT(SEND_MESSAGE, markType = DELETE),
    SAME_CENTER(SEND_MESSAGE, markType = DELETE),
    SEND_REPORT_COMPLETE(SEND_MESSAGE),
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
    VERIFICATION,
    NOTHING,
}

enum class MarkType {
    DELETE,
    UPDATE,
    NONE,
}
