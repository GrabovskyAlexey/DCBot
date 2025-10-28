package ru.grabovsky.dungeoncrusherbot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.User as TgUser
import ru.grabovsky.dungeoncrusherbot.entity.*
import ru.grabovsky.dungeoncrusherbot.service.interfaces.AdjustType
import ru.grabovsky.dungeoncrusherbot.service.interfaces.GoogleFormService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ResourceOperation
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ResourcesService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import java.time.LocalDate

@Service
class ResourcesServiceImpl(
    private val userService: UserService,
    private val googleFormService: GoogleFormService,
) : ResourcesService {
    override fun applyOperation(user: TgUser, serverId: Int, operation: ResourceOperation) {
        val userFromDb = userService.getUser(user.id)
            ?: throw EntityNotFoundException("User with id: ${user.id} not found")
        val profile = userFromDb.profile
            ?: throw IllegalStateException("Profile not initialized for user: ${user.id}")
        val resources = userFromDb.resources
            ?: throw IllegalStateException("Resources not initialized for user: ${user.userName ?: user.firstName}")

        val serverData = resources.data.servers.computeIfAbsent(serverId) { ServerResourceData() }
        val history = resources.history.computeIfAbsent(serverId) { mutableListOf() }

        when (operation) {
            is ResourceOperation.Adjust -> handleAdjustOperation(
                user = userFromDb,
                resources = resources,
                serverData = serverData,
                history = history,
                type = operation.type,
                amount = operation.amount,
                serverId = serverId,
            )

            is ResourceOperation.SetExchange -> serverData.exchange = operation.value.trim().takeIf { it.isNotEmpty() }
            ResourceOperation.ClearExchange -> serverData.exchange = null
            ResourceOperation.ToggleNotify -> serverData.notifyDisable = !serverData.notifyDisable
            ResourceOperation.MarkMain -> profile.mainServerId = serverId
            ResourceOperation.UnmarkMain -> profile.mainServerId = null
        }

        if (shouldNotifyWatermelon(operation, serverId, userFromDb)) {
            logger.info { "Send info to Watermelon for user: ${userFromDb.userName ?: userFromDb.firstName}" }
            googleFormService.sendDraadorCount(operationAmount(operation).toString(), profile.settings.discordUsername!!)
        }

        userService.saveUser(userFromDb)
    }

    private fun handleAdjustOperation(
        user: User,
        resources: Resources,
        serverData: ServerResourceData,
        history: MutableList<ResourcesHistory>,
        type: AdjustType,
        amount: Int,
        serverId: Int,
    ) {
        require(amount > 0) { "Amount must be positive" }
        when (type) {
            AdjustType.ADD_VOID -> {
                serverData.voidCount += amount
                addHistory(history, ResourceType.VOID, DirectionType.ADD, amount)
            }

            AdjustType.REMOVE_VOID -> {
                serverData.voidCount -= amount
                addHistory(history, ResourceType.VOID, DirectionType.REMOVE, amount)
            }

            AdjustType.ADD_CB -> {
                serverData.cbCount += amount
                addHistory(history, ResourceType.CB, DirectionType.ADD, amount)
            }

            AdjustType.REMOVE_CB -> {
                serverData.cbCount -= amount
                addHistory(history, ResourceType.CB, DirectionType.REMOVE, amount)
            }

            AdjustType.ADD_DRAADOR -> {
                serverData.draadorCount += amount
                addHistory(history, ResourceType.DRAADOR, DirectionType.CATCH, amount)
            }

            AdjustType.SELL_DRAADOR -> {
                serverData.draadorCount -= amount
                if (serverData.draadorCount < 0) serverData.draadorCount = 0
                addHistory(history, ResourceType.DRAADOR, DirectionType.TRADE, amount)
            }

            AdjustType.SEND_DRAADOR -> {
                serverData.draadorCount -= amount
                serverData.balance += amount
                if (serverData.draadorCount < 0) serverData.draadorCount = 0
                addHistory(history, ResourceType.DRAADOR, DirectionType.OUTGOING, amount)
            }

            AdjustType.RECEIVE_DRAADOR -> {
                receiveDraador(user, resources, serverId, amount)
            }
        }
    }

    private fun receiveDraador(user: User, resources: Resources, serverId: Int, amount: Int) {
        val serverData = resources.data.servers.computeIfAbsent(serverId) { ServerResourceData() }
        val history = resources.history.computeIfAbsent(serverId) { mutableListOf() }
        serverData.balance -= amount
        addHistory(history, ResourceType.DRAADOR, DirectionType.INCOMING, amount)

        val profile = user.profile ?: return
        val mainServerId = profile.mainServerId ?: return
        val mainServer = resources.data.servers.computeIfAbsent(mainServerId) { ServerResourceData() }
        val mainHistory = resources.history.computeIfAbsent(mainServerId) { mutableListOf() }
        mainServer.draadorCount += amount
        addHistory(mainHistory, ResourceType.DRAADOR, DirectionType.INCOMING, amount, serverId)
    }

    private fun addHistory(
        history: MutableList<ResourcesHistory>,
        resourceType: ResourceType,
        directionType: DirectionType,
        amount: Int,
        fromServer: Int? = null,
    ) {
        if (history.size >= 20) {
            history.removeFirst()
        }
        history.add(
            ResourcesHistory(
                date = LocalDate.now(),
                resource = resourceType,
                type = directionType,
                quantity = amount,
                fromServer = fromServer
            )
        )
    }

    private fun shouldNotifyWatermelon(operation: ResourceOperation, serverId: Int, user: User): Boolean {
        if (operation !is ResourceOperation.Adjust) {
            return false
        }
        if (operation.type != AdjustType.ADD_DRAADOR) {
            return false
        }
        val settings = user.profile?.settings ?: return false
        return serverId == 8 && settings.sendWatermelon && settings.discordUsername != null
    }

    private fun operationAmount(operation: ResourceOperation): Int =
        (operation as? ResourceOperation.Adjust)?.amount ?: 0

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
