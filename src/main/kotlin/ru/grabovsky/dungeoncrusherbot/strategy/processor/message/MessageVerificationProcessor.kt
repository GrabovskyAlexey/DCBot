package ru.grabovsky.dungeoncrusherbot.strategy.processor.message

import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.entity.VerificationRequest
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService

abstract class MessageVerificationProcessor(private val stateService: StateService) : MessageProcessor {
    override fun execute(user: User, message: Message) {
        verify(user, message)
    }

    fun verify(user: User, message: Message) {
        val state = stateService.getState(user)
        state.verification = state.verification?.apply{
            this.message = message.text
            this.stateCode = state.state
        } ?: VerificationRequest(message = message.text, stateCode = state.state)
        stateService.saveState(state)
    }
}