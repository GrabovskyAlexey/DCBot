package ru.grabovsky.dungeoncrusherbot.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.grabovsky.dungeoncrusherbot.entity.ResourceServerState

interface ResourceServerStateRepository : JpaRepository<ResourceServerState, Long> {
    fun findAllByUserUserId(userId: Long): List<ResourceServerState>
    fun findByUserUserIdAndServerId(userId: Long, serverId: Int): ResourceServerState?
    fun findAllByUserUserIdAndNotifyDisableTrue(userId: Long): List<ResourceServerState>
    fun findAllByNotifyDisableTrue(): List<ResourceServerState>
    fun findAllByServerIdAndExchangeUserId(serverId: Int, exchangeUserId: Long): List<ResourceServerState>
    fun findAllByServerIdAndExchangeUsernameIgnoreCase(serverId: Int, exchangeUsername: String): List<ResourceServerState>
    fun findAllByExchangeUsernameIgnoreCase(exchangeUsername: String): List<ResourceServerState>
    fun findAllByExchangeUserId(exchangeUserId: Long): List<ResourceServerState>
}
