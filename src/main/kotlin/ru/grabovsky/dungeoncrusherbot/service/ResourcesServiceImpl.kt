package ru.grabovsky.dungeoncrusherbot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import ru.grabovsky.dungeoncrusherbot.entity.*
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.*
import java.time.LocalDate
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Service
class ResourcesServiceImpl(
    private val userService: UserService,
    private val googleFormService: GoogleFormService,
    private val resourceStateSyncService: ResourceStateSyncService,
    private val userRepository: UserRepository,
) : ResourcesService {
    override fun undoLastOperation(user: TgUser, serverId: Int): Boolean {
        val userFromDb = userService.getUser(user.id) ?: return false
        val resources = userFromDb.resources ?: return false
        val history = resources.history[serverId] ?: return false
        val profile = userFromDb.profile
        val mainId = profile?.mainServerId
        val idx = history.indexOfLast { entry ->
            !(mainId != null &&
                mainId == serverId &&
                entry.resource == ResourceType.DRAADOR &&
                entry.type == DirectionType.INCOMING &&
                entry.fromServer != null)
        }
        if (idx == -1) {
            return false
        }
        val entry = history.removeAt(idx)
        val serverData = resources.data.servers.computeIfAbsent(serverId) { ServerResourceData() }

        when (entry.resource) {
            ResourceType.DRAADOR -> undoDraador(entry, serverData, resources, userFromDb, serverId)
            ResourceType.VOID -> undoVoid(entry, serverData)
            ResourceType.CB -> undoCb(entry, serverData)
        }

        resourceStateSyncService.syncStateFromLegacy(userFromDb, serverId, serverData)
        resourceStateSyncService.removeHistoryEntry(userFromDb, serverId, entry)
        userService.saveUser(userFromDb)
        return true
    }
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
            ResourceOperation.ClearExchange -> {
                serverData.exchange = null
                serverData.exchangeUsername = null
                serverData.exchangeUserId = null
            }
            is ResourceOperation.SetExchangeUsername -> {
                val normalized = operation.value.trim().removePrefix("@").trim()
                serverData.exchangeUsername = normalized.takeIf { it.isNotEmpty() }
                serverData.exchangeUserId = serverData.exchangeUsername
                    ?.let { userRepository.findByUserNameIgnoreCase(it) }
                    ?.userId
            }
            ResourceOperation.ClearExchangeUsername -> {
                serverData.exchangeUsername = null
                serverData.exchangeUserId = null
            }
            ResourceOperation.ToggleNotify -> serverData.notifyDisable = !serverData.notifyDisable
            ResourceOperation.MarkMain -> profile.mainServerId = serverId
            ResourceOperation.UnmarkMain -> profile.mainServerId = null
        }

        if (shouldNotifyWatermelon(operation, serverId, userFromDb)) {
            logger.info { "Send info to Watermelon for user: ${userFromDb.userName ?: userFromDb.firstName}" }
            googleFormService.sendDraadorCount(operationAmount(operation).toString(), profile.settings.discordUsername!!)
        }

        resourceStateSyncService.syncStateFromLegacy(userFromDb, serverId, serverData)
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
                addHistory(user, serverId, serverData, history, ResourceType.VOID, DirectionType.ADD, amount)
            }

            AdjustType.REMOVE_VOID -> {
                serverData.voidCount -= amount
                addHistory(user, serverId, serverData, history, ResourceType.VOID, DirectionType.REMOVE, amount)
            }

            AdjustType.ADD_CB -> {
                serverData.cbCount += amount
                addHistory(user, serverId, serverData, history, ResourceType.CB, DirectionType.ADD, amount)
            }

            AdjustType.REMOVE_CB -> {
                serverData.cbCount -= amount
                addHistory(user, serverId, serverData, history, ResourceType.CB, DirectionType.REMOVE, amount)
            }

            AdjustType.ADD_DRAADOR -> {
                serverData.draadorCount += amount
                addHistory(user, serverId, serverData, history, ResourceType.DRAADOR, DirectionType.CATCH, amount)
            }

            AdjustType.SELL_DRAADOR -> {
                serverData.draadorCount -= amount
                if (serverData.draadorCount < 0) serverData.draadorCount = 0
                addHistory(user, serverId, serverData, history, ResourceType.DRAADOR, DirectionType.TRADE, amount)
            }

            AdjustType.SEND_DRAADOR -> {
                serverData.draadorCount -= amount
                serverData.balance += amount
                if (serverData.draadorCount < 0) serverData.draadorCount = 0
                addHistory(user, serverId, serverData, history, ResourceType.DRAADOR, DirectionType.OUTGOING, amount)
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
        addHistory(user, serverId, serverData, history, ResourceType.DRAADOR, DirectionType.INCOMING, amount)

        val profile = user.profile ?: return
        if(profile.settings.enableMainSend) {
            val mainServerId = profile.mainServerId ?: return
            val mainServer = resources.data.servers.computeIfAbsent(mainServerId) { ServerResourceData() }
            val mainHistory = resources.history.computeIfAbsent(mainServerId) { mutableListOf() }
            mainServer.draadorCount += amount
            addHistory(user, mainServerId, mainServer, mainHistory, ResourceType.DRAADOR, DirectionType.INCOMING, amount, serverId)
            resourceStateSyncService.syncStateFromLegacy(user, mainServerId, mainServer)
        }
    }

    private fun addHistory(
        user: User,
        serverId: Int,
        serverData: ServerResourceData,
        history: MutableList<ResourcesHistory>,
        resourceType: ResourceType,
        directionType: DirectionType,
        amount: Int,
        fromServer: Int? = null,
    ) {
        if (history.size >= 20) {
            history.removeFirst()
        }
        val entry = ResourcesHistory(
            date = LocalDate.now(),
            resource = resourceType,
            type = directionType,
            quantity = amount,
            fromServer = fromServer
        )
        history.add(entry)
        resourceStateSyncService.appendHistoryFromLegacy(user, serverId, serverData, entry)
    }

    private fun undoDraador(
        entry: ResourcesHistory,
        serverData: ServerResourceData,
        resources: Resources,
        user: User,
        serverId: Int
    ) {
        when (entry.type) {
            DirectionType.ADD, DirectionType.CATCH -> {
                serverData.draadorCount -= entry.quantity
                if (serverData.draadorCount < 0) serverData.draadorCount = 0
            }
            DirectionType.TRADE -> serverData.draadorCount += entry.quantity
            DirectionType.OUTGOING -> {
                serverData.draadorCount += entry.quantity
                serverData.balance -= entry.quantity
            }
            DirectionType.INCOMING -> {
                serverData.balance += entry.quantity
            }
            DirectionType.REMOVE -> serverData.draadorCount += entry.quantity
        }

        val mainId = user.profile?.mainServerId
        if (entry.type == DirectionType.INCOMING && mainId != null && mainId != serverId) {
            val mainData = resources.data.servers.computeIfAbsent(mainId) { ServerResourceData() }
            val mainHistory = resources.history.computeIfAbsent(mainId) { mutableListOf() }
            val idx = mainHistory.indexOfLast {
                it.resource == ResourceType.DRAADOR &&
                    it.type == DirectionType.INCOMING &&
                    it.fromServer == serverId &&
                    it.quantity == entry.quantity
            }
            if (idx >= 0) {
                val removedEntry = mainHistory.removeAt(idx)
                mainData.draadorCount -= removedEntry.quantity
                if (mainData.draadorCount < 0) mainData.draadorCount = 0
                resourceStateSyncService.syncStateFromLegacy(user, mainId, mainData)
                resourceStateSyncService.removeHistoryEntry(user, mainId, removedEntry)
            }
        }
    }

    private fun undoVoid(entry: ResourcesHistory, serverData: ServerResourceData) {
        when (entry.type) {
            DirectionType.ADD -> serverData.voidCount -= entry.quantity
            DirectionType.REMOVE -> serverData.voidCount += entry.quantity
            else -> {}
        }
        if (serverData.voidCount < 0) serverData.voidCount = 0
    }

    private fun undoCb(entry: ResourcesHistory, serverData: ServerResourceData) {
        when (entry.type) {
            DirectionType.ADD -> serverData.cbCount -= entry.quantity
            DirectionType.REMOVE -> serverData.cbCount += entry.quantity
            else -> {}
        }
        if (serverData.cbCount < 0) serverData.cbCount = 0
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
