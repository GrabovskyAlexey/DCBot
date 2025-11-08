package ru.grabovsky.dungeoncrusherbot.strategy.flow.resources

import ru.grabovsky.dungeoncrusherbot.service.interfaces.AdjustType

enum class AmountActionType {
    ADD_DRAADOR,
    SELL_DRAADOR,
    SEND_DRAADOR,
    RECEIVE_DRAADOR,
    ADD_VOID,
    REMOVE_VOID,
    ADD_CB,
    REMOVE_CB;

    fun toAdjustType(): AdjustType = when (this) {
        ADD_DRAADOR -> AdjustType.ADD_DRAADOR
        SELL_DRAADOR -> AdjustType.SELL_DRAADOR
        SEND_DRAADOR -> AdjustType.SEND_DRAADOR
        RECEIVE_DRAADOR -> AdjustType.RECEIVE_DRAADOR
        ADD_VOID -> AdjustType.ADD_VOID
        REMOVE_VOID -> AdjustType.REMOVE_VOID
        ADD_CB -> AdjustType.ADD_CB
        REMOVE_CB -> AdjustType.REMOVE_CB
    }
}