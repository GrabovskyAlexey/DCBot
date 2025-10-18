package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.subscribe

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.CallbackProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.ExecuteStatus
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Component
class SubscribeProcessor(
    private val userService: UserService,
    private val serverService: ServerService,
    stateService: StateService
): CallbackProcessor(stateService) {
    override fun process(
        user: TgUser,
        callbackData: String
    ): ExecuteStatus {
        val data = callbackData.split(" ")
        val server = serverService.getServerById(data[1].toInt())
        val userFromDb = userService.getUser(user.id)
            ?: User(user.id, user.firstName, user.lastName, user.userName)
        when(data[0]) {
            "SUBSCRIBE" -> userFromDb.servers.add(server)
            "UNSUBSCRIBE" -> userFromDb.servers.removeIf { it == server }
        }
        userService.saveUser(userFromDb)
        return ExecuteStatus.FINAL
    }
}