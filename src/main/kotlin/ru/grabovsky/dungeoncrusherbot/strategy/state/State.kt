package ru.grabovsky.dungeoncrusherbot.strategy.state

import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.util.CommonUtils.currentStateCode


interface State {
    fun isAvailableForCurrentState(stateCode: StateCode): Boolean {
        return this.currentStateCode( "State") == stateCode
    }

    fun getNextState(user: User): StateCode?
}