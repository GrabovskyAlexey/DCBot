package ru.grabovsky.dungeoncrusherbot.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.*
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ResourcesService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.*
import java.time.LocalDate

@Service
class ResourcesServiceImpl(
    private val userService: UserService
) : ResourcesService {

    override fun processResources(user: User, value: String, state: StateCode) {
        val userFromDb =
            userService.getUser(user.id) ?: throw EntityNotFoundException("User with id: ${user.id} not found")
        val resources = userFromDb.resources
        val lastServerId = resources?.lastServerId
            ?: throw IllegalStateException("Not found last server id for resources user: ${user.userName ?: user.firstName}")
        val serverData = resources.data.servers.computeIfAbsent(lastServerId) { key -> ServerResourceData() }
        val history = resources.history.computeIfAbsent(lastServerId) { key -> (mutableListOf()) }

        when (state) {
            ADD_VOID, REMOVE_VOID -> processVoid(serverData, value, history, state)
            ADD_DRAADOR, SELL_DRAADOR, SEND_DRAADOR -> processDraador(serverData, value, history, state)
            RECEIVE_DRAADOR -> receiveDraador(resources, value, lastServerId)
            ADD_EXCHANGE -> serverData.exchange = value
            else -> {}
        }

        userService.saveUser(userFromDb)
    }

    private fun processVoid(
        serverData: ServerResourceData,
        value: String,
        history: MutableList<ResourcesHistory>,
        state: StateCode
    ) {
        val amount = value.toInt()
        when (state) {
            ADD_VOID -> {
                serverData.voidCount += amount
                updateHistory(
                    history, ResourcesHistory(
                        LocalDate.now(),
                        ResourceType.VOID,
                        DirectionType.ADD,
                        amount
                    )
                )
            }

            REMOVE_VOID -> {
                serverData.voidCount -= amount
                updateHistory(
                    history, ResourcesHistory(
                        LocalDate.now(),
                        ResourceType.VOID,
                        DirectionType.REMOVE,
                        amount
                    )
                )
            }

            else -> {}
        }
    }

    private fun processDraador(
        lastServerData: ServerResourceData,
        value: String,
        history: MutableList<ResourcesHistory>,
        state: StateCode
    ) {
        val amount = value.toInt()
        when (state) {
            ADD_DRAADOR -> {
                lastServerData.draadorCount += amount
                updateHistory(
                    history, ResourcesHistory(
                        LocalDate.now(),
                        ResourceType.DRAADOR,
                        DirectionType.CATCH,
                        amount
                    )
                )
            }

            SELL_DRAADOR -> {
                lastServerData.draadorCount -= amount
                if (lastServerData.draadorCount < 0) lastServerData.draadorCount = 0
                updateHistory(
                    history, ResourcesHistory(
                        LocalDate.now(),
                        ResourceType.DRAADOR,
                        DirectionType.TRADE,
                        amount
                    )
                )
            }

            SEND_DRAADOR -> {
                lastServerData.draadorCount -= amount
                lastServerData.balance += amount
                if (lastServerData.draadorCount < 0) lastServerData.draadorCount = 0
                updateHistory(
                    history, ResourcesHistory(
                        LocalDate.now(),
                        ResourceType.DRAADOR,
                        DirectionType.OUTGOING,
                        amount
                    )
                )
            }

            else -> {}
        }
    }

    private fun receiveDraador(resources: Resources, value: String, lastServerId: Int) {
        val amount = value.toInt()
        val serverData = resources.data.servers.computeIfAbsent(lastServerId) { key -> ServerResourceData() }
        val history = resources.history.computeIfAbsent(lastServerId) { key -> (mutableListOf()) }
        serverData.balance -= amount
        updateHistory(
            history, ResourcesHistory(
                LocalDate.now(),
                ResourceType.DRAADOR,
                DirectionType.INCOMING,
                amount
            )
        )
        val mainServerId = resources.data.mainServerId
        if (mainServerId != null) {
            val data = resources.data.servers.computeIfAbsent(mainServerId) { key -> ServerResourceData() }
            data.draadorCount += amount
            updateHistory(resources.history.computeIfAbsent(mainServerId) { key -> (mutableListOf()) },
                ResourcesHistory(
                    LocalDate.now(),
                    ResourceType.DRAADOR,
                    DirectionType.INCOMING,
                    amount,
                    lastServerId
                )
            )
        }
    }


    private fun updateHistory(history: MutableList<ResourcesHistory>, item: ResourcesHistory) {
        if (history.size >= 20) {
            history.removeFirst()
        }
        history.addLast(item)
    }
}