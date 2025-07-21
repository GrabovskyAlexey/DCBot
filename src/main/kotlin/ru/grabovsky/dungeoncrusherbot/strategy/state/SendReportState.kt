package ru.grabovsky.dungeoncrusherbot.strategy.state

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User

@Component
class SendReportState : State {
    override fun getNextState(user: User): StateCode? = StateCode.WAITING
}