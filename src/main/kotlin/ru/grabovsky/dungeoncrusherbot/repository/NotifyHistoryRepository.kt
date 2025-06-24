package ru.grabovsky.dungeoncrusherbot.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.grabovsky.dungeoncrusherbot.entity.NotifyHistory
import java.time.Instant

interface NotifyHistoryRepository: JpaRepository<NotifyHistory, Long> {
    fun findNotifyHistoryBySendTimeBefore(sendTime: Instant): List<NotifyHistory>
    fun findNotifyHistoryByDeletedNot(isDeleted: Boolean = true): List<NotifyHistory>
}