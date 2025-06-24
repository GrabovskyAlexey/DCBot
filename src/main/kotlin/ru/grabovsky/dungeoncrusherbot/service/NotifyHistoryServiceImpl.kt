package ru.grabovsky.dungeoncrusherbot.service

import org.springframework.stereotype.Service
import ru.grabovsky.dungeoncrusherbot.entity.NotifyHistory
import ru.grabovsky.dungeoncrusherbot.repository.NotifyHistoryRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.NotifyHistoryService
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class NotifyHistoryServiceImpl(
    private val notifyHistoryRepository: NotifyHistoryRepository
) : NotifyHistoryService {
    override fun saveHistory(history: NotifyHistory) {
        notifyHistoryRepository.saveAndFlush(history)
    }

    override fun getNotDeletedHistoryEvent(): List<NotifyHistory> {
        return notifyHistoryRepository.findNotifyHistoryByDeletedNot(true)
    }

    override fun markAsDeleted(histories: List<NotifyHistory>) {
        histories.onEach { it.deleted = true }
            .also { notifyHistoryRepository.saveAllAndFlush(it) }
    }

    override fun deleteOldEvents() {
        val removeDate = Instant.now().minus(30, ChronoUnit.DAYS)
        notifyHistoryRepository.findNotifyHistoryBySendTimeBefore(removeDate)
            .also { notifyHistoryRepository.deleteAll(it) }
    }
}