package ru.grabovsky.dungeoncrusherbot.strategy.state

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User

@Component
class SameRightState : State {
    override fun getNextState(user: User): StateCode? = StateCode.VERIFY
}