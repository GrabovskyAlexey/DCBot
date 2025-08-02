package ru.grabovsky.dungeoncrusherbot.strategy.data.resources

import org.springframework.stereotype.Repository
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.data.AbstractDataRepository
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ResourceDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerResourceDto

@Repository
class ResourcesDataRepository(
    private val userService: UserService
) : AbstractDataRepository<ResourceDto>() {
    override fun getData(user: User): ResourceDto {
        val userFromDb = userService.getUser(user.id)
        val resources = userFromDb?.resources
        val cbEnabled = userFromDb?.settings?.resourcesCb ?: false
        return resources?.let { res ->
            res.data.servers.filter { it.value.hasData(cbEnabled) || it.key == res.data.mainServerId }
                .map {
                    ServerResourceDto(
                        it.key,
                        it.value.draadorCount,
                        it.value.voidCount,
                        it.value.balance,
                        it.value.exchange,
                        notifyDisable = it.value.notifyDisable,
                        main = it.key == res.data.mainServerId,
                        cbEnabled = cbEnabled,
                        cbCount = it.value.cbCount,
                        hasMain = res.data.mainServerId != null
                    )
                }
        }?.let { ResourceDto(it) } ?: ResourceDto()
    }
}