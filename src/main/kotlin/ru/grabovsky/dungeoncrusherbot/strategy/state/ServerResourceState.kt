package ru.grabovsky.dungeoncrusherbot.strategy.state

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.*

@Component
class ServerResourceState(private val stateService: StateService) : State {
    override fun getNextState(user: User): StateCode? {
        val state = stateService.getState(user)
        return when(state.callbackData) {
            "BACK" -> UPDATE_RESOURCES
            "REMOVE_EXCHANGE" -> REMOVE_EXCHANGE
            "ADD_EXCHANGE" -> ADD_EXCHANGE
            "ADD_VOID" -> ADD_VOID
            "REMOVE_VOID" -> REMOVE_VOID
            "ADD_CB" -> ADD_CB
            "REMOVE_CB" -> REMOVE_CB
            "ADD_DRAADOR" -> ADD_DRAADOR
            "SEND_DRAADOR" -> SEND_DRAADOR
            "RECEIVE_DRAADOR" -> RECEIVE_DRAADOR
            "SELL_DRAADOR" -> SELL_DRAADOR
            "ADD_NOTE" -> ADD_NOTE
            "REMOVE_NOTE" -> REMOVE_NOTE
            else -> SERVER_RESOURCE
        }
    }
}








