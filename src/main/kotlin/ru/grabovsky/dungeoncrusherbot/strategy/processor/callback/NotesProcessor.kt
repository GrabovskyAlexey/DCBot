package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.entity.NotificationSubscribe
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.*
import org.telegram.telegrambots.meta.api.objects.User as tgUser

@Component
class NotesProcessor(
    stateService: StateService,
    private val userService: UserService,
) : CallbackProcessor(stateService) {
    override fun process(
        user: tgUser,
        callbackData: String
    ): ExecuteStatus {
        val state = stateService.getState(user)
        state.prevState = UPDATE_NOTES

        when(callbackData) {
            "CLEAR_NOTES" -> userService.clearNotes(user)
        }
        stateService.saveState(state)
        return ExecuteStatus.FINAL
    }
}