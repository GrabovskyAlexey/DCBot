package ru.grabovsky.dungeoncrusherbot.strategy.state

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService

@Component
class VerifyState(private val stateService: StateService) : State {
    override fun getNextState(user: User): StateCode? {
        val isVerified = stateService.getState(user).verification?.result ?: false
        return if (isVerified) {
            StateCode.VERIFICATION_SUCCESS
        } else {
            StateCode.VERIFICATION_ERROR
        }
    }
}