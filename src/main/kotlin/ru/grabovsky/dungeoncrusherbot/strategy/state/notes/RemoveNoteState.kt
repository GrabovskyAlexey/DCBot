package ru.grabovsky.dungeoncrusherbot.strategy.state.notes

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.strategy.state.State
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

@Component
class RemoveNoteState : State {
    override fun getNextState(user: User): StateCode? = StateCode.VERIFY
}