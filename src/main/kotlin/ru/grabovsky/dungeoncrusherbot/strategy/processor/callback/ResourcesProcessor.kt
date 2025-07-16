package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Component
class ResourcesProcessor(
    private val userService: UserService,
    stateService: StateService
): CallbackProcessor(stateService) {
    override fun process(
        user: TgUser,
        callbackData: String
    ): ExecuteStatus {
        val data = callbackData.split(" ")

        val userFromDb = userService.getUser(user.id)
            ?: User(user.id, user.firstName, user.lastName, user.userName)
        when(data[0]) {
            "RESOURCE" -> userFromDb.resources?.lastServerId = data[1].toInt()
        }
        userService.saveUser(userFromDb)
        return ExecuteStatus.FINAL
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}