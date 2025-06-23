package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Component
class SubscribeProcessor(
    private val userService: UserService,
    private val serverService: ServerService,
    stateService: StateService
): CallbackProcessor(stateService) {
    override fun process(
        user: TgUser,
        callbackQuery: CallbackQuery
    ): ExecuteStatus {
        val data = callbackQuery.data.split(" ")
        val server = serverService.getServerById(data[1].toInt())
        val tgUser = callbackQuery.from
        val user = userService.getUser(tgUser.id)
            ?: User(tgUser.id, tgUser.firstName, tgUser.lastName, tgUser.userName)
        when(data[0]) {
            "SUBSCRIBE" -> user.servers.add(server)
            "UNSUBSCRIBE" -> user.servers.removeIf { it == server }
        }
        userService.saveUser(user)
        return ExecuteStatus.FINAL
    }
}