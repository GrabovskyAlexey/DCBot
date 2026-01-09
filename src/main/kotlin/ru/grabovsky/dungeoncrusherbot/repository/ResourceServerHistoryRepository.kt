package ru.grabovsky.dungeoncrusherbot.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.grabovsky.dungeoncrusherbot.entity.ResourceServerHistory

interface ResourceServerHistoryRepository : JpaRepository<ResourceServerHistory, Long> {
    fun findAllByServerStateIdAndIsDeletedFalseOrderByIdAsc(id: Long): List<ResourceServerHistory>
    fun findTop20ByServerStateIdAndIsDeletedFalseOrderByIdDesc(id: Long): List<ResourceServerHistory>
    fun deleteAllByServerStateId(id: Long)
}
