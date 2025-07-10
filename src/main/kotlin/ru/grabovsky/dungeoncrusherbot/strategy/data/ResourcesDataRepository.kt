package ru.grabovsky.dungeoncrusherbot.strategy.data

import org.springframework.stereotype.Repository
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ResourceDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerResourceDto

@Repository
class ResourcesDataRepository(
    private val userService: UserService
): AbstractDataRepository<ResourceDto>() {
    override fun getData(user: User): ResourceDto {
        val resources = userService.getUser(user.id)?.resources
        return resources?.let {res ->
            res.data.servers.filterValues { it.hasData() }
                .map { ServerResourceDto(it.key, it.value.draadorCount, it.value.voidCount, 0,it.value.exchange) }
        }?.let { ResourceDto(it) } ?: ResourceDto()
    }
}