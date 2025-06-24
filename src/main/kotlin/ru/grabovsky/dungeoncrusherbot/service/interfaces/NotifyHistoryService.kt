package ru.grabovsky.dungeoncrusherbot.service.interfaces

import ru.grabovsky.dungeoncrusherbot.entity.NotifyHistory

interface NotifyHistoryService {
    fun saveHistory(history: NotifyHistory)
    fun getNotDeletedHistoryEvent(): List<NotifyHistory>
    fun markAsDeleted(histories: List<NotifyHistory>)
    fun deleteOldEvents()
}