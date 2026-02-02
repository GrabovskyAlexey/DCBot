package ru.grabovsky.dungeoncrusherbot.service

import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import ru.grabovsky.dungeoncrusherbot.entity.*
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequestType.*
import ru.grabovsky.dungeoncrusherbot.repository.ExchangeRequestRepository
import ru.grabovsky.dungeoncrusherbot.service.events.ExchangeSearchPerformedEvent
import ru.grabovsky.dungeoncrusherbot.service.events.GlobalExchangeSearchPerformedEvent
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ExchangeRequestService

@Service
class ExchangeRequestServiceImpl(
    private val exchangeRequestRepository: ExchangeRequestRepository,
    private val eventPublisher: ApplicationEventPublisher,
) : ExchangeRequestService {

    override fun getActiveExchangeRequestsByServer(
        user: User,
        serverId: Int,
        serverType: ExchangeDirectionType
    ): List<ExchangeRequest> {
        return when (serverType) {
            ExchangeDirectionType.SOURCE -> exchangeRequestRepository.findAllByUserAndSourceServerId(user, serverId)
            ExchangeDirectionType.TARGET -> exchangeRequestRepository.findAllByUserAndTargetServerId(user, serverId)
        }
    }

    @Transactional
    override fun getActiveExchangeRequestsByServerExcludeSelfExchange(
        user: User,
        serverId: Int,
        serverType: ExchangeDirectionType
    ): List<ExchangeRequest> {
        val all =  when (serverType) {
            ExchangeDirectionType.SOURCE -> exchangeRequestRepository.findAllBySourceServerId(serverId)
            ExchangeDirectionType.TARGET -> exchangeRequestRepository.findAllByTargetServerId(serverId)
        }
            .filter { it.isActive }
            .filterNot { it.user == user }
            .filter {it.user.isActiveAndHasUsername()}
        val self = getActiveExchangeRequestsByServer(user, serverId, ExchangeDirectionType.SOURCE).filter { it.isActive }
        val filtered = filterRequests(all, self)
        publishSearchEvent(user, serverId, serverType, filtered)
        return filtered
    }

    override fun createOrUpdateExchangeRequest(
        user: User,
        exchangeRequestType: ExchangeRequestType,
        sourceServerId: Int,
        targetServerId: Int?,
        sourceResourceType: ExchangeResourceType,
        targetResourceType: ExchangeResourceType,
        sourcePrice: Int,
        targetPrice: Int,
    ) {
        val request = exchangeRequestRepository.findByUserAndSourceServerIdAndTargetServerIdAndType(
            user,
            sourceServerId,
            targetServerId,
            exchangeRequestType
        )
        if (request == null) {
            exchangeRequestRepository.save(
                ExchangeRequest(
                    type = exchangeRequestType,
                    user = user,
                    sourceServerId = sourceServerId,
                    targetServerId = targetServerId,
                    sourceResourceType = sourceResourceType,
                    targetResourceType = targetResourceType,
                    sourceResourcePrice = sourcePrice,
                    targetResourcePrice = targetPrice,
                    isActive = true
                )
            )
            return
        }

        request.apply {
            this.type = exchangeRequestType
            this.targetServerId = targetServerId
            this.sourceResourceType = sourceResourceType
            this.targetResourceType = targetResourceType
            this.sourceResourcePrice = sourcePrice
            this.targetResourcePrice = targetPrice
            this.isActive = true
        }.also { exchangeRequestRepository.save(it) }
    }

    override fun findRequestsByServerAndType(
        serverId: Int,
        type: ExchangeRequestType
    ): List<ExchangeRequest> {
        return exchangeRequestRepository.findAllBySourceServerIdAndType(serverId, type)
    }

    override fun setRequestInactiveById(requestId: Long) {
        val request = exchangeRequestRepository.findRequestById(requestId) ?: return
        request.isActive = false
        exchangeRequestRepository.saveAndFlush(request)
    }

    override fun getRequestById(requestId: Long) =
         exchangeRequestRepository.findRequestById(requestId)

    override fun getActiveExchangeRequestsByUser(user: User): List<ExchangeRequest> {
        return exchangeRequestRepository.findAllByUser(user).filter { it.isActive }
    }

    @Transactional
    override fun getGlobalExchangeMatches(user: User): Map<ExchangeRequest, List<ExchangeRequest>> {
        val userRequests = getActiveExchangeRequestsByUser(user)
            .filter { it.isActive }

        if (userRequests.isEmpty()) {
            return emptyMap()
        }

        // Собрать все уникальные серверы из заявок пользователя
        val sourceServerIds = userRequests.map { it.sourceServerId }.toSet()
        val targetServerIds = userRequests.mapNotNull { it.targetServerId }.toSet()
        val allServerIds = sourceServerIds + targetServerIds

        // Получить ВСЕ активные заявки для этих серверов ОДНИМ запросом
        val allPotentialMatches = exchangeRequestRepository.findAllBySourceServerIdIn(allServerIds.toList())
            .filter { it.isActive }
            .filterNot { it.user == user }
            .filter { it.user.isActiveAndHasUsername() }

        // Также получить заявки где наши серверы в target
        val allPotentialMatchesTarget = if (allServerIds.isNotEmpty()) {
            exchangeRequestRepository.findAllByTargetServerIdIn(allServerIds.toList())
                .filter { it.isActive }
                .filterNot { it.user == user }
                .filter { it.user.isActiveAndHasUsername() }
        } else {
            emptyList()
        }

        val allMatches = (allPotentialMatches + allPotentialMatchesTarget).distinctBy { it.id }

        // Теперь фильтруем в памяти для каждой заявки пользователя
        val results = mutableMapOf<ExchangeRequest, List<ExchangeRequest>>()

        userRequests.forEach { request ->
            // Для каждой заявки пользователя фильтруем все совпадения
            // используя существующую логику filterRequests
            val filtered = filterRequests(allMatches, listOf(request))
            if (filtered.isNotEmpty()) {
                results[request] = filtered
            }
        }

        publishGlobalSearchEvent(user, results)

        return results
    }

    private fun publishGlobalSearchEvent(
        user: User,
        results: Map<ExchangeRequest, List<ExchangeRequest>>
    ) {
        val totalMatches = results.values.sumOf { it.size }
        val userRequestsCount = results.keys.size

        eventPublisher.publishEvent(
            GlobalExchangeSearchPerformedEvent(
                userId = user.userId,
                userRequestsCount = userRequestsCount,
                totalMatchesCount = totalMatches,
                matchesPerRequest = results.mapKeys { it.key.id!! }.mapValues { it.value.size }
            )
        )
    }

    private fun filterRequests(all: List<ExchangeRequest>, self: List<ExchangeRequest>): List<ExchangeRequest> {
        val results = mutableListOf<ExchangeRequest>()

        self.forEach { userRequest ->
            when (userRequest.type) {
                SELL_MAP -> {
                    // Пользователь продает карты на сервере X по курсу A:B
                    // Ищем тех, кто покупает карты на том же сервере X по курсу >= A:B
                    all.filter { it.type == BUY_MAP && it.sourceServerId == userRequest.sourceServerId }
                        .filter {
                            // Проверяем что курс совпадает или выгоднее
                            it.targetResourcePrice == userRequest.targetResourcePrice &&
                            it.sourceResourcePrice == userRequest.sourceResourcePrice
                        }
                        .forEach { results.add(it) }
                }

                BUY_MAP -> {
                    // Пользователь покупает карты на сервере X по курсу A:B
                    // Ищем тех, кто продает карты на том же сервере X по курсу <= A:B
                    all.filter { it.type == SELL_MAP && it.sourceServerId == userRequest.sourceServerId }
                        .filter {
                            // Проверяем что курс совпадает или выгоднее
                            it.sourceResourcePrice == userRequest.targetResourcePrice &&
                            it.targetResourcePrice == userRequest.sourceResourcePrice
                        }
                        .forEach { results.add(it) }
                }

                EXCHANGE_MAP -> {
                    // Пользователь хочет обменять карты: сервер A → сервер B
                    // Ищем тех, кто хочет обменять: сервер B → сервер A (обратное направление)
                    all.filter { it.type == EXCHANGE_MAP }
                        .filter {
                            it.sourceServerId == userRequest.targetServerId &&
                            it.targetServerId == userRequest.sourceServerId
                        }
                        .forEach { results.add(it) }
                }

                EXCHANGE_VOID -> {
                    // Пользователь хочет обменять пустоты: сервер A → сервер B
                    // Ищем тех, кто хочет обменять: сервер B → сервер A (обратное направление)
                    all.filter { it.type == EXCHANGE_VOID }
                        .filter {
                            it.sourceServerId == userRequest.targetServerId &&
                            it.targetServerId == userRequest.sourceServerId
                        }
                        .forEach { results.add(it) }
                }
            }
        }

        return results.distinct()
    }

    private fun publishSearchEvent(
        user: User,
        serverId: Int,
        direction: ExchangeDirectionType,
        matches: List<ExchangeRequest>,
    ) {
        val typeCounts = matches.groupingBy { it.type }.eachCount()
        eventPublisher.publishEvent(
            ExchangeSearchPerformedEvent(
                userId = user.userId,
                serverId = serverId,
                direction = direction,
                matchesCount = matches.size,
                matchTypeCounts = typeCounts,
            )
        )
    }
}
