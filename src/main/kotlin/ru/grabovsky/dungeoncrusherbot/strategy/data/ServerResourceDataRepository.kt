package ru.grabovsky.dungeoncrusherbot.strategy.data

import org.springframework.stereotype.Repository
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.ServerResourceData
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerResourceDto
import ru.grabovsky.dungeoncrusherbot.util.CommonUtils.currentStateCode

@Repository
class ServerResourceDataRepository(
    private val userService: UserService,
    private val stateService: StateService
): AbstractDataRepository<ServerResourceDto>() {
    override fun getData(user: User): ServerResourceDto {
        val userFromDb = userService.getUser(user.id)
        val resources = userFromDb?.resources
        val lastServerId = resources?.lastServerId ?:throw IllegalStateException("Not found last server id for resources user: ${user.userName ?: user.firstName}")
        val serverData = resources.data.servers.computeIfAbsent(lastServerId) {key -> ServerResourceData()}
        val state = stateService.getState(user)

        val history = resources.history[lastServerId]
        val historyItems = if (state.callbackData == "RESOURCE_HISTORY") {
            stateService.updateState(user, currentStateCode("DataRepository"))
            history?.map { it.toString() }
        } else {
            null
        }
        userService.saveUser(userFromDb)
        return ServerResourceDto(lastServerId, serverData.draadorCount, serverData.voidCount, serverData.balance, serverData.exchange, historyItems, history != null)
    }
}