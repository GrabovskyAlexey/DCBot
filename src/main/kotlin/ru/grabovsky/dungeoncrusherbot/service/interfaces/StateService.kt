package ru.grabovsky.dungeoncrusherbot.service.interfaces

import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

interface StateService {
    fun updateState(user: User, code: StateCode)
    fun getState(user: User): UserState
    fun saveState(state: UserState): UserState
}
