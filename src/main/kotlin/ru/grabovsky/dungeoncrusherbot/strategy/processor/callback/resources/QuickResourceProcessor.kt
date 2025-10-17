package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.resources

import org.springframework.util.Assert.state
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ResourcesService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.CallbackProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.ExecuteStatus
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

abstract class QuickResourceProcessor(
    private val userService: UserService,
    private val resourcesService: ResourcesService,
    stateService: StateService,
    private val resourceState: StateCode,
) : CallbackProcessor(stateService) {
    override fun process(user: TgUser, callbackData: String): ExecuteStatus {
        val userFromDb = userService.getUser(user.id) ?: return ExecuteStatus.NOTHING
        if (!userFromDb.settings.resourcesQuickChange) {
            return ExecuteStatus.NOTHING
        }
        if (requiresCbEnabled() && !userFromDb.settings.resourcesCb) {
            return ExecuteStatus.NOTHING
        }
        val state = stateService.getState(user)
        val resources = userFromDb.resources ?: return ExecuteStatus.NOTHING
        state.lastServerIdByState[StateCode.RESOURCES]
            ?: resources.lastServerId
            ?: return ExecuteStatus.NOTHING

        resourcesService.processResources(user, "1", resourceState)
        state.prevState = StateCode.UPDATE_SERVER_RESOURCE
        stateService.saveState(state)
        return ExecuteStatus.FINAL
    }

    protected open fun requiresCbEnabled(): Boolean = false
}

