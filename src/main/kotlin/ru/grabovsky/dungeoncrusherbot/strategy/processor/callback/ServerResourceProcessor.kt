package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.entity.ServerResourceData
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Component
class ServerResourceProcessor(
    private val userService: UserService,
    stateService: StateService,
) : CallbackProcessor(stateService) {
    override fun process(
        user: TgUser,
        callbackData: String
    ): ExecuteStatus {
        val data = callbackData.split(" ")

        val userFromDb = userService.getUser(user.id)
            ?: User(user.id, user.firstName, user.lastName, user.userName)
        val lastServerId = userFromDb.resources?.lastServerId
            ?: return ExecuteStatus.NOTHING.also { logger.warn { "Not found last server id for resources user: ${user.userName ?: user.firstName}" } }
        val serverResource =
            userFromDb.resources?.data?.servers?.computeIfAbsent(lastServerId) { key -> ServerResourceData() }
                ?: return ExecuteStatus.NOTHING.also { logger.warn { "Not found server resource by id: $lastServerId for user: ${user.userName ?: user.firstName}" } }
        when (data[0]) {
            "REMOVE_EXCHANGE" -> serverResource.exchange = null
        }
        userService.saveUser(userFromDb)
        return ExecuteStatus.FINAL
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}