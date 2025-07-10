package ru.grabovsky.dungeoncrusherbot.strategy.state

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat

@Component
class VerificationSuccessState : State {
    override fun getNextState(user: User): StateCode? = StateCode.SERVER_RESOURCE
}