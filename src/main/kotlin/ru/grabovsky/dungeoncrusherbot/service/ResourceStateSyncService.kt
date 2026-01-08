package ru.grabovsky.dungeoncrusherbot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import ru.grabovsky.dungeoncrusherbot.entity.*
import ru.grabovsky.dungeoncrusherbot.repository.ResourceServerHistoryRepository
import ru.grabovsky.dungeoncrusherbot.repository.ResourceServerStateRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService

@Service
class ResourceStateSyncService(
    private val resourceServerStateRepository: ResourceServerStateRepository,
    private val resourceServerHistoryRepository: ResourceServerHistoryRepository,
    private val serverService: ServerService,
) {
    fun getServerDataMap(user: User): Map<Int, ServerResourceData> {
        val result = resourceServerStateRepository.findAllByUserUserId(user.userId)
            .associate { it.server.id to it.toServerResourceData() }
            .toMutableMap()
        val mainServerId = user.profile?.mainServerId
        if (mainServerId != null && !result.containsKey(mainServerId)) {
            result[mainServerId] = ServerResourceData()
        }
        return result
    }

    fun getServerSnapshot(user: User, serverId: Int): Pair<ServerResourceData, List<ResourcesHistory>> {
        val newState = resourceServerStateRepository.findByUserUserIdAndServerId(user.userId, serverId)
        val data = newState?.toServerResourceData() ?: ServerResourceData()
        val history = newState?.id
            ?.let { getHistoryEntries(it) }
            ?.map { it.toDomain() }
            ?: emptyList()
        return data to history
    }

    fun getOrCreateState(user: User, serverId: Int): ResourceServerState {
        val existing = resourceServerStateRepository.findByUserUserIdAndServerId(user.userId, serverId)
        if (existing != null) {
            return existing
        }
        val server = serverService.getServerById(serverId)
        return resourceServerStateRepository.save(ResourceServerState(user = user, server = server))
    }

    fun findState(userId: Long, serverId: Int): ResourceServerState? =
        resourceServerStateRepository.findByUserUserIdAndServerId(userId, serverId)

    fun saveState(state: ResourceServerState) {
        resourceServerStateRepository.save(state)
    }

    fun appendHistory(state: ResourceServerState, entry: ResourcesHistory) {
        resourceServerHistoryRepository.save(
            ResourceServerHistory(
                serverState = state,
                eventDate = entry.date,
                resource = entry.resource,
                direction = entry.type,
                quantity = entry.quantity,
                fromServer = entry.fromServer,
                prevDraadorCount = entry.prevDraadorCount,
                prevVoidCount = entry.prevVoidCount,
                prevCbCount = entry.prevCbCount,
                prevBalance = entry.prevBalance,
            )
        )
    }

    fun findByExchangeUserId(serverId: Int, exchangeUserId: Long): List<ResourceServerState> =
        resourceServerStateRepository.findAllByServerIdAndExchangeUserId(serverId, exchangeUserId)

    fun findByExchangeUsername(serverId: Int, exchangeUsername: String): List<ResourceServerState> =
        resourceServerStateRepository.findAllByServerIdAndExchangeUsernameIgnoreCase(serverId, exchangeUsername)

    fun getHistoryEntries(stateId: Long): List<ResourceServerHistory> {
        val history = resourceServerHistoryRepository.findTop20ByServerStateIdAndIsDeletedFalseOrderByIdDesc(stateId)
        return history.asReversed()
    }

    fun removeHistoryEntry(state: ResourceServerState, entry: ResourcesHistory) {
        val history = resourceServerHistoryRepository.findAllByServerStateIdAndIsDeletedFalseOrderByIdAsc(state.id!!)
        val target = history.lastOrNull {
            it.resource == entry.resource &&
                it.direction == entry.type &&
                it.quantity == entry.quantity &&
                it.fromServer == entry.fromServer
        } ?: return
        target.isDeleted = true
        resourceServerHistoryRepository.save(target)
    }

    private fun ResourceServerState.toServerResourceData(): ServerResourceData = ServerResourceData(
        exchange = exchangeLabel,
        exchangeUsername = exchangeUsername?.removePrefix("@"),
        exchangeUserId = exchangeUserId,
        draadorCount = draadorCount,
        voidCount = voidCount,
        balance = balance,
        notifyDisable = notifyDisable,
        cbCount = cbCount,
    )

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

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
