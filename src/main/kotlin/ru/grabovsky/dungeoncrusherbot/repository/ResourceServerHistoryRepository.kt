package ru.grabovsky.dungeoncrusherbot.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.grabovsky.dungeoncrusherbot.entity.ResourceServerHistory

interface ResourceServerHistoryRepository : JpaRepository<ResourceServerHistory, Long> {
    fun findAllByServerStateIdOrderByIdAsc(id: Long): List<ResourceServerHistory>
}
