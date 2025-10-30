package ru.grabovsky.dungeoncrusherbot.service.interfaces

import ru.grabovsky.dungeoncrusherbot.entity.*
interface ExchangeRequestService {
    fun getActiveExchangeRequestsByServer(user: User, serverId: Int, serverType: ExchangeDirectionType): List<ExchangeRequest>
    fun getActiveExchangeRequestsByServerExcludeSelfExchange(user: User, serverId: Int, serverType: ExchangeDirectionType): List<ExchangeRequest>
    fun createOrUpdateExchangeRequest(
        user: User,
        exchangeRequestType: ExchangeRequestType,
        sourceServerId: Int,
        targetServerId: Int? = null,
        sourceResourceType: ExchangeResourceType,
        targetResourceType: ExchangeResourceType,
        sourcePrice: Int,
        targetPrice: Int,
    )

    fun findRequestsByServerAndType(serverId: Int, type: ExchangeRequestType): List<ExchangeRequest>
    fun setRequestInactiveById(requestId: Long)
    fun getRequestById(requestId: Long): ExchangeRequest?
    fun getActiveExchangeRequestsByUser(user: User): List<ExchangeRequest>
}
