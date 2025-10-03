package ru.grabovsky.dungeoncrusherbot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequest
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeRequestType
import ru.grabovsky.dungeoncrusherbot.entity.User

@Repository
interface ExchangeRequestRepository: JpaRepository<ExchangeRequest, Long> {
    fun findAllByUserAndSourceServerId(user: User, serviceId: Int): List<ExchangeRequest>
    fun findAllByUserAndTargetServerId(user: User, serviceId: Int): List<ExchangeRequest>
    fun findAllBySourceServerIdAndType(sourceServerId: Int, type: ExchangeRequestType): List<ExchangeRequest>
    fun findByUserAndSourceServerIdAndType(user: User, serverId: Int, type: ExchangeRequestType): ExchangeRequest?
}