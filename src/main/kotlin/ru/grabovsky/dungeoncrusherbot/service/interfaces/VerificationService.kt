package ru.grabovsky.dungeoncrusherbot.service.interfaces

import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

interface VerificationService {
    fun verify(user: User, stateCode: StateCode)
}