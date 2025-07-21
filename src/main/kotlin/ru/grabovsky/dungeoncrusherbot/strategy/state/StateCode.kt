package ru.grabovsky.dungeoncrusherbot.strategy.state

import ru.grabovsky.dungeoncrusherbot.strategy.state.MarkType.*
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateAction.*

enum class StateCode(val action: StateAction, val pause: Boolean = true, val template: String? = null, val markType: MarkType = NONE, val linkedStateCode: StateCode? = null) {
    START(SEND_MESSAGE, false),
    SUBSCRIBE(SEND_MESSAGE, markType = UPDATE),
    UPDATE_SUBSCRIBE(UPDATE_MESSAGE, linkedStateCode = SUBSCRIBE),
    MAZE(SEND_MESSAGE, markType = UPDATE),
    UPDATE_MAZE(UPDATE_MESSAGE, linkedStateCode = MAZE),
    CONFIRM_REFRESH_MAZE(UPDATE_MESSAGE, linkedStateCode = MAZE),
    HELP(SEND_MESSAGE, false),
    RESOURCES(SEND_MESSAGE, markType = UPDATE),
    UPDATE_RESOURCES(UPDATE_MESSAGE, linkedStateCode = RESOURCES),
    SETTINGS(SEND_MESSAGE, markType = UPDATE),
    UPDATE_SETTINGS(UPDATE_MESSAGE, linkedStateCode = SETTINGS),
    NOTES(SEND_MESSAGE, markType = UPDATE),
    UPDATE_NOTES(UPDATE_MESSAGE, linkedStateCode = NOTES),
    VERIFY(VERIFICATION, false),
    VERIFICATION_ERROR(SEND_MESSAGE, false, markType = DELETE),
    VERIFICATION_SUCCESS(DELETE_MESSAGES,false),
    SERVER_RESOURCE(UPDATE_MESSAGE, linkedStateCode = RESOURCES),
    UPDATE_SERVER_RESOURCE(UPDATE_MESSAGE, linkedStateCode = RESOURCES),
    ADD_EXCHANGE(SEND_MESSAGE, markType = DELETE),
    REMOVE_EXCHANGE(NOTHING, pause = false),
    ADD_VOID(SEND_MESSAGE, markType = DELETE),
    REMOVE_VOID(SEND_MESSAGE, markType = DELETE),
    ADD_CB(SEND_MESSAGE, markType = DELETE),
    REMOVE_CB(SEND_MESSAGE, markType = DELETE),
    ADD_DRAADOR(SEND_MESSAGE, markType = DELETE),
    SELL_DRAADOR(SEND_MESSAGE, markType = DELETE),
    SEND_DRAADOR(SEND_MESSAGE, markType = DELETE),
    RECEIVE_DRAADOR(SEND_MESSAGE, markType = DELETE),
    ADD_NOTE(SEND_MESSAGE, markType = DELETE),
    REMOVE_NOTE(SEND_MESSAGE, markType = DELETE),
    SEND_REPORT(SEND_MESSAGE, markType = DELETE),
    SAME_LEFT(SEND_MESSAGE, markType = DELETE),
    SAME_RIGHT(SEND_MESSAGE, markType = DELETE),
    SAME_CENTER(SEND_MESSAGE, markType = DELETE),
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
    NOTHING
}

enum class MarkType{
    DELETE, UPDATE, NONE
}