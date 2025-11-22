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
        val legacyData = legacyServerData(user)
        val newStates = resourceServerStateRepository.findAllByUserUserId(user.userId)
            .associateBy { it.server.id }

        val serverIds = (legacyData.keys + newStates.keys).toSet()
        val result = mutableMapOf<Int, ServerResourceData>()
        serverIds.forEach { serverId ->
            val oldData = legacyData[serverId]
            val newData = newStates[serverId]?.toServerResourceData()
            when {
                newData == null -> {
                    if (oldData != null) {
                        logger.error { "New resource state missing for user=${user.userId}, server=$serverId" }
                    }
                    result[serverId] = oldData ?: ServerResourceData()
                }

                oldData == null -> {
                    logger.warn { "Legacy resource data missing for user=${user.userId}, server=$serverId, using legacy defaults" }
                    result[serverId] = ServerResourceData()
                }

                newData != oldData -> {
                    logger.warn { "Resource state mismatch for user=${user.userId}, server=$serverId, returning legacy data" }
                    result[serverId] = oldData
                }

                else -> result[serverId] = newData
            }
        }
        return result
    }

    fun getServerSnapshot(user: User, serverId: Int): Pair<ServerResourceData, List<ResourcesHistory>> {
        val legacyData = legacyServerData(user)[serverId] ?: ServerResourceData()
        val legacyHistory = legacyHistory(user)[serverId].orEmpty()

        val newState = resourceServerStateRepository.findByUserUserIdAndServerId(user.userId, serverId)
        val newData = newState?.toServerResourceData()
        val newHistory = newState?.id?.let { resourceServerHistoryRepository.findAllByServerStateIdOrderByIdAsc(it) }
            ?.map { it.toDomain() }

        val data = when {
            newData == null -> {
                if (!legacyData.isEmpty()) {
                    logger.error { "New resource state missing for user=${user.userId}, server=$serverId" }
                }
                legacyData
            }

            newData != legacyData -> {
                logger.warn { "Resource state mismatch for user=${user.userId}, server=$serverId, returning legacy data" }
                legacyData
            }

            else -> newData
        }

        val history = when {
            newHistory == null -> {
                if (legacyHistory.isNotEmpty()) {
                    logger.error { "New resource history missing for user=${user.userId}, server=$serverId" }
                }
                legacyHistory
            }

            newHistory != legacyHistory -> {
                logger.warn { "Resource history mismatch for user=${user.userId}, server=$serverId, returning legacy history" }
                legacyHistory
            }

            else -> newHistory
        }

        return data to history
    }

    fun syncStateFromLegacy(user: User, serverId: Int, legacyData: ServerResourceData) {
        val server = serverService.getServerById(serverId)
        val state = resourceServerStateRepository.findByUserUserIdAndServerId(user.userId, serverId)
            ?: ResourceServerState(user = user, server = server)

        state.exchangeLabel = legacyData.exchange
        state.exchangeUsername = legacyData.exchangeUsername?.removePrefix("@")
        state.exchangeUserId = legacyData.exchangeUserId
        state.draadorCount = legacyData.draadorCount
        state.voidCount = legacyData.voidCount
        state.cbCount = legacyData.cbCount
        state.balance = legacyData.balance
        state.notifyDisable = legacyData.notifyDisable

        resourceServerStateRepository.save(state)
    }

    fun appendHistoryFromLegacy(
        user: User,
        serverId: Int,
        serverData: ServerResourceData,
        legacyEntry: ResourcesHistory
    ) {
        val state = resourceServerStateRepository.findByUserUserIdAndServerId(user.userId, serverId)
            ?: resourceServerStateRepository.save(
                ResourceServerState(
                    user = user,
                    server = serverService.getServerById(serverId),
                    exchangeLabel = serverData.exchange,
                    exchangeUsername = serverData.exchangeUsername?.removePrefix("@"),
                    exchangeUserId = serverData.exchangeUserId,
                    draadorCount = serverData.draadorCount,
                    voidCount = serverData.voidCount,
                    cbCount = serverData.cbCount,
                    balance = serverData.balance,
                    notifyDisable = serverData.notifyDisable,
                )
            )

        resourceServerHistoryRepository.save(
            ResourceServerHistory(
                serverState = state,
                eventDate = legacyEntry.date,
                resource = legacyEntry.resource,
                direction = legacyEntry.type,
                quantity = legacyEntry.quantity,
                fromServer = legacyEntry.fromServer,
            )
        )
    }

    fun findByExchangeUserId(serverId: Int, exchangeUserId: Long): List<ResourceServerState> =
        resourceServerStateRepository.findAllByServerIdAndExchangeUserId(serverId, exchangeUserId)

    fun findByExchangeUsername(serverId: Int, exchangeUsername: String): List<ResourceServerState> =
        resourceServerStateRepository.findAllByServerIdAndExchangeUsernameIgnoreCase(serverId, exchangeUsername)

    @Deprecated("Legacy JSON resources storage")
    private fun legacyServerData(user: User): Map<Int, ServerResourceData> =
        user.resources?.data?.servers ?: emptyMap()

    @Deprecated("Legacy JSON resources storage")
    private fun legacyHistory(user: User): Map<Int, List<ResourcesHistory>> =
        user.resources?.history ?: emptyMap()

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

    private fun ServerResourceData.isEmpty(): Boolean =
        exchange == null && exchangeUsername == null && exchangeUserId == null && draadorCount == 0 && voidCount == 0 && cbCount == 0 && balance == 0 && !notifyDisable

    private fun ResourceServerHistory.toDomain(): ResourcesHistory = ResourcesHistory(
        date = eventDate,
        resource = resource,
        type = direction,
        quantity = quantity,
        fromServer = fromServer
    )

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
