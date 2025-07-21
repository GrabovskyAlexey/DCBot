package ru.grabovsky.dungeoncrusherbot.strategy.state

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.*

@Component
class SettingsState(private val stateService: StateService) : State {
    override fun getNextState(user: User): StateCode? {
        val state = stateService.getState(user)
        return when(state.callbackData) {
            "SEND_REPORT" -> SEND_REPORT
            else -> UPDATE_SETTINGS
        }
    }
}