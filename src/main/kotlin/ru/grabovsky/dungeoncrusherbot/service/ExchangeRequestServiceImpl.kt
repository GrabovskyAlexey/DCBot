package ru.grabovsky.dungeoncrusherbot.service

import org.springframework.stereotype.Service
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeDirectionType
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequest
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequestType
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeResourceType
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.repository.ExchangeRequestRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ExchangeRequestService

@Service
class ExchangeRequestServiceImpl(
    private val exchangeRequestRepository: ExchangeRequestRepository
) : ExchangeRequestService {
    override fun getExchangeRequestsByServer(
        user: User,
        serverId: Int,
        serverType: ExchangeDirectionType
    ): List<ExchangeRequest> {
        return when (serverType) {
            ExchangeDirectionType.SOURCE -> exchangeRequestRepository.findAllByUserAndSourceServerId(user, serverId)
            ExchangeDirectionType.TARGET -> exchangeRequestRepository.findAllByUserAndTargetServerId(user, serverId)
        }
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
        val request = exchangeRequestRepository.findByUserAndSourceServerIdAndType(user, sourceServerId, exchangeRequestType)
        if (request == null) {
            ExchangeRequest(
                type = exchangeRequestType,
                user = user,
                sourceServerId = sourceServerId,
                targetServerId = targetServerId,
                sourceResourceType = sourceResourceType,
                targetResourceType = targetResourceType,
                sourceResourcePrice = sourcePrice,
                targetResourcePrice = targetPrice,
                isActive = true,
            ).also { exchangeRequestRepository.save(it) }
            return
        }
             request.apply {
                type = exchangeRequestType
                sourceResourcePrice = sourcePrice
                targetResourcePrice = targetPrice
                isActive = true
             }.also { exchangeRequestRepository.save(it) }

    }

    override fun findRequestsByServerAndType(
        serverId: Int,
        type: ExchangeRequestType
    ): List<ExchangeRequest> {
        return exchangeRequestRepository.findAllBySourceServerIdAndType(serverId, type)
    }
}