package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.settings

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.entity.NotificationSubscribe
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.CallbackProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.ExecuteStatus
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.UPDATE_SETTINGS
import org.telegram.telegrambots.meta.api.objects.User as tgUser

@Component
class SettingsProcessor(
    private val userService: UserService,
    stateService: StateService,
) : CallbackProcessor(stateService) {
    override fun process(
        user: tgUser,
        callbackData: String,
    ): ExecuteStatus {
        val userFromDb = userService.getUser(user.id)!!
        val state = stateService.getState(user)
        state.prevState = UPDATE_SETTINGS
        stateService.saveState(state)
        when (callbackData) {
            "NOTIFY_SIEGE" -> processNotify(userFromDb, NotificationType.SIEGE)
            "NOTIFY_MINE" -> processNotify(userFromDb, NotificationType.MINE)
            "CB_ENABLE" -> userFromDb.settings.resourcesCb = !userFromDb.settings.resourcesCb
            "QUICK_RESOURCES" -> userFromDb.settings.resourcesQuickChange = !userFromDb.settings.resourcesQuickChange
        }

        userService.saveUser(userFromDb)
        return ExecuteStatus.FINAL
    }

    private fun processNotify(user: User, type: NotificationType) {
        val notify = user.notificationSubscribe.firstOrNull { it.type == type }
        if (notify != null) {
            notify.enabled = !notify.enabled
        } else {
            user.notificationSubscribe.add(
                NotificationSubscribe(
                    user = user,
                    type = type,
                    enabled = true,
                ),
            )
        }
    }
}
