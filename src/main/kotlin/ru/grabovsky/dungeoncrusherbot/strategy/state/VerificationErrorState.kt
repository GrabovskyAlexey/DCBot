package ru.grabovsky.dungeoncrusherbot.strategy.state

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService

@Component
class VerificationErrorState(private val stateService: StateService) : State {
    override fun getNextState(user: User): StateCode? {
        return stateService.getState(user).verification?.stateCode
    }
}