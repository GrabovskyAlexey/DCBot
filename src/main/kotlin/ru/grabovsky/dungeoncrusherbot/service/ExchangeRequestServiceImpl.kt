package ru.grabovsky.dungeoncrusherbot.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import ru.grabovsky.dungeoncrusherbot.entity.*
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequestType.*
import ru.grabovsky.dungeoncrusherbot.repository.ExchangeRequestRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ExchangeRequestService

@Service
class ExchangeRequestServiceImpl(
    private val exchangeRequestRepository: ExchangeRequestRepository,
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
        return filterRequests(all, self)
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

    private fun filterRequests(all: List<ExchangeRequest>, self: List<ExchangeRequest>): List<ExchangeRequest> {
        val buyRequests = if (self.any{it.type == SELL_MAP }) {
            all.filter{ it.type == BUY_MAP }
        } else {
            emptyList()
        }

        val sellRequests = if (self.any{it.type == BUY_MAP }) {
            all.filter{ it.type == SELL_MAP }
        } else {
            emptyList()
        }

        val mapRequest = self.filter { it.type == EXCHANGE_MAP }
        val voidRequest = self.filter { it.type == EXCHANGE_VOID }
        val exchangeMapRequest = all.filter { it.type == EXCHANGE_MAP }.filter {mapRequest.any { req -> req.targetServerId == it.sourceServerId } }
        val exchangeVoidRequest = all.filter { it.type == EXCHANGE_VOID }.filter {voidRequest.any { req -> req.targetServerId == it.sourceServerId } }

        return buyRequests + sellRequests + exchangeMapRequest + exchangeVoidRequest
    }
}
