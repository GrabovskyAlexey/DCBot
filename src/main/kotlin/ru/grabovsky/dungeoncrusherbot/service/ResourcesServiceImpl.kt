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
        val profile = userFromDb.profile
            ?: return false
        val state = resourceStateSyncService.findState(userFromDb.userId, serverId)
            ?: return false
        val history = resourceStateSyncService.getHistoryEntries(state.id!!)
            .map { it.toDomain() }
            .toMutableList()
        if (history.isEmpty()) {
            return false
        }
        val mainId = profile.mainServerId
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
        val entry = history[idx]

        when (entry.resource) {
            ResourceType.DRAADOR -> undoDraador(entry, state, userFromDb, serverId)
            ResourceType.VOID -> undoVoid(entry, state)
            ResourceType.CB -> undoCb(entry, state)
        }

        resourceStateSyncService.saveState(state)
        resourceStateSyncService.removeHistoryEntry(state, entry)
        userService.saveUser(userFromDb)
        return true
    }
    override fun applyOperation(user: TgUser, serverId: Int, operation: ResourceOperation) {
        val userFromDb = userService.getUser(user.id)
            ?: throw EntityNotFoundException("User with id: ${user.id} not found")
        val profile = userFromDb.profile
            ?: throw IllegalStateException("Profile not initialized for user: ${user.id}")
        val state = resourceStateSyncService.getOrCreateState(userFromDb, serverId)

        when (operation) {
            is ResourceOperation.Adjust -> handleAdjustOperation(
                user = userFromDb,
                state = state,
                type = operation.type,
                amount = operation.amount,
                serverId = serverId,
            )

            is ResourceOperation.SetExchange -> state.exchangeLabel = operation.value.trim().takeIf { it.isNotEmpty() }
            ResourceOperation.ClearExchange -> {
                state.exchangeLabel = null
                state.exchangeUsername = null
                state.exchangeUserId = null
            }
            is ResourceOperation.SetExchangeUsername -> {
                val normalized = operation.value.trim().removePrefix("@").trim()
                state.exchangeUsername = normalized.takeIf { it.isNotEmpty() }
                state.exchangeUserId = state.exchangeUsername
                    ?.let { userRepository.findByUserNameIgnoreCase(it) }
                    ?.userId
            }
            ResourceOperation.ClearExchangeUsername -> {
                state.exchangeUsername = null
                state.exchangeUserId = null
            }
            ResourceOperation.ToggleNotify -> state.notifyDisable = !state.notifyDisable
            ResourceOperation.MarkMain -> profile.mainServerId = serverId
            ResourceOperation.UnmarkMain -> profile.mainServerId = null
        }

        if (shouldNotifyWatermelon(operation, serverId, userFromDb)) {
            logger.info { "Send info to Watermelon for user: ${userFromDb.userName ?: userFromDb.firstName}" }
            googleFormService.sendDraadorCount(operationAmount(operation).toString(), profile.settings.discordUsername!!)
        }

        if (operation !is ResourceOperation.Adjust) {
            resourceStateSyncService.saveState(state)
        }
        userService.saveUser(userFromDb)
    }

    private fun handleAdjustOperation(
        user: User,
        state: ResourceServerState,
        type: AdjustType,
        amount: Int,
        serverId: Int,
    ) {
        require(amount > 0) { "Amount must be positive" }
        when (type) {
            AdjustType.ADD_VOID -> {
                val prevVoid = state.voidCount
                state.voidCount += amount
                addHistory(
                    state = state,
                    resourceType = ResourceType.VOID,
                    directionType = DirectionType.ADD,
                    amount = amount,
                    prevVoidCount = prevVoid
                )
            }

            AdjustType.REMOVE_VOID -> {
                val prevVoid = state.voidCount
                state.voidCount -= amount
                addHistory(
                    state = state,
                    resourceType = ResourceType.VOID,
                    directionType = DirectionType.REMOVE,
                    amount = amount,
                    prevVoidCount = prevVoid
                )
            }

            AdjustType.ADD_CB -> {
                val prevCb = state.cbCount
                state.cbCount += amount
                addHistory(
                    state = state,
                    resourceType = ResourceType.CB,
                    directionType = DirectionType.ADD,
                    amount = amount,
                    prevCbCount = prevCb
                )
            }

            AdjustType.REMOVE_CB -> {
                val prevCb = state.cbCount
                state.cbCount -= amount
                addHistory(
                    state = state,
                    resourceType = ResourceType.CB,
                    directionType = DirectionType.REMOVE,
                    amount = amount,
                    prevCbCount = prevCb
                )
            }

            AdjustType.ADD_DRAADOR -> {
                val prevDraador = state.draadorCount
                state.draadorCount += amount
                addHistory(
                    state = state,
                    resourceType = ResourceType.DRAADOR,
                    directionType = DirectionType.CATCH,
                    amount = amount,
                    prevDraadorCount = prevDraador
                )
            }

            AdjustType.SELL_DRAADOR -> {
                val prevDraador = state.draadorCount
                state.draadorCount -= amount
                if (state.draadorCount < 0) state.draadorCount = 0
                addHistory(
                    state = state,
                    resourceType = ResourceType.DRAADOR,
                    directionType = DirectionType.TRADE,
                    amount = amount,
                    prevDraadorCount = prevDraador
                )
            }

            AdjustType.SEND_DRAADOR -> {
                val prevDraador = state.draadorCount
                val prevBalance = state.balance
                state.draadorCount -= amount
                state.balance += amount
                if (state.draadorCount < 0) state.draadorCount = 0
                addHistory(
                    state = state,
                    resourceType = ResourceType.DRAADOR,
                    directionType = DirectionType.OUTGOING,
                    amount = amount,
                    prevDraadorCount = prevDraador,
                    prevBalance = prevBalance
                )
            }

            AdjustType.RECEIVE_DRAADOR -> {
                val prevBalance = state.balance
                receiveDraador(user, state, serverId, amount, prevBalance)
            }
        }
        resourceStateSyncService.saveState(state)
    }

    private fun receiveDraador(
        user: User,
        state: ResourceServerState,
        serverId: Int,
        amount: Int,
        prevBalance: Int
    ) {
        state.balance -= amount
        addHistory(
            state = state,
            resourceType = ResourceType.DRAADOR,
            directionType = DirectionType.INCOMING,
            amount = amount,
            prevBalance = prevBalance
        )
        resourceStateSyncService.saveState(state)

        val profile = user.profile ?: return
        if(profile.settings.enableMainSend) {
            val mainServerId = profile.mainServerId ?: return
            val mainState = resourceStateSyncService.getOrCreateState(user, mainServerId)
            val prevDraador = mainState.draadorCount
            mainState.draadorCount += amount
            addHistory(
                state = mainState,
                resourceType = ResourceType.DRAADOR,
                directionType = DirectionType.INCOMING,
                amount = amount,
                fromServer = serverId,
                prevDraadorCount = prevDraador
            )
            resourceStateSyncService.saveState(mainState)
        }
    }

    private fun addHistory(
        state: ResourceServerState,
        resourceType: ResourceType,
        directionType: DirectionType,
        amount: Int,
        fromServer: Int? = null,
        prevDraadorCount: Int? = null,
        prevVoidCount: Int? = null,
        prevCbCount: Int? = null,
        prevBalance: Int? = null,
    ) {
        val entry = ResourcesHistory(
            date = LocalDate.now(),
            resource = resourceType,
            type = directionType,
            quantity = amount,
            fromServer = fromServer,
            prevDraadorCount = prevDraadorCount,
            prevVoidCount = prevVoidCount,
            prevCbCount = prevCbCount,
            prevBalance = prevBalance,
        )
        resourceStateSyncService.appendHistory(state, entry)
    }

    private fun undoDraador(
        entry: ResourcesHistory,
        state: ResourceServerState,
        user: User,
        serverId: Int
    ) {
        val hasPrevDraador = entry.prevDraadorCount != null
        val hasPrevBalance = entry.prevBalance != null
        if (hasPrevDraador) {
            state.draadorCount = entry.prevDraadorCount!!
        }
        if (hasPrevBalance) {
            state.balance = entry.prevBalance!!
        }
        if (!hasPrevDraador && !hasPrevBalance) {
            when (entry.type) {
                DirectionType.ADD, DirectionType.CATCH -> {
                    state.draadorCount -= entry.quantity
                    if (state.draadorCount < 0) state.draadorCount = 0
                }
                DirectionType.TRADE -> state.draadorCount += entry.quantity
                DirectionType.OUTGOING -> {
                    state.draadorCount += entry.quantity
                    state.balance -= entry.quantity
                }
                DirectionType.INCOMING -> {
                    state.balance += entry.quantity
                }
                DirectionType.REMOVE -> state.draadorCount += entry.quantity
            }
        }

        val mainId = user.profile?.mainServerId
        if (entry.type == DirectionType.INCOMING && mainId != null && mainId != serverId) {
            val mainState = resourceStateSyncService.getOrCreateState(user, mainId)
            val mainHistory = resourceStateSyncService.getHistoryEntries(mainState.id!!)
                .map { it.toDomain() }
            val removedEntry = mainHistory.lastOrNull {
                it.resource == ResourceType.DRAADOR &&
                    it.type == DirectionType.INCOMING &&
                    it.fromServer == serverId &&
                    it.quantity == entry.quantity
            }
            if (removedEntry != null) {
                if (removedEntry.prevDraadorCount != null) {
                    mainState.draadorCount = removedEntry.prevDraadorCount
                } else {
                    mainState.draadorCount -= removedEntry.quantity
                    if (mainState.draadorCount < 0) mainState.draadorCount = 0
                }
                resourceStateSyncService.saveState(mainState)
                resourceStateSyncService.removeHistoryEntry(mainState, removedEntry)
            }
        }
    }

    private fun undoVoid(entry: ResourcesHistory, state: ResourceServerState) {
        if (entry.prevVoidCount != null) {
            state.voidCount = entry.prevVoidCount
            return
        }
        when (entry.type) {
            DirectionType.ADD -> state.voidCount -= entry.quantity
            DirectionType.REMOVE -> state.voidCount += entry.quantity
            else -> {}
        }
        if (state.voidCount < 0) state.voidCount = 0
    }

    private fun undoCb(entry: ResourcesHistory, state: ResourceServerState) {
        if (entry.prevCbCount != null) {
            state.cbCount = entry.prevCbCount
            return
        }
        when (entry.type) {
            DirectionType.ADD -> state.cbCount -= entry.quantity
            DirectionType.REMOVE -> state.cbCount += entry.quantity
            else -> {}
        }
        if (state.cbCount < 0) state.cbCount = 0
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

    private fun ResourceServerHistory.toDomain(): ResourcesHistory = ResourcesHistory(
        date = eventDate,
        resource = resource,
        type = direction,
        quantity = quantity,
        fromServer = fromServer,
        prevDraadorCount = prevDraadorCount,
        prevVoidCount = prevVoidCount,
        prevCbCount = prevCbCount,
        prevBalance = prevBalance,
    )
}
