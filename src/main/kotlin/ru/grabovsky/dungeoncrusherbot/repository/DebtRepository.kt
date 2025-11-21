package ru.grabovsky.dungeoncrusherbot.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.grabovsky.dungeoncrusherbot.entity.Debt

interface DebtRepository : JpaRepository<Debt, Long> {
    fun findAllByUserUserIdOrderByCreatedAtDesc(userId: Long): List<Debt>
    fun existsByIdAndUserUserId(id: Long, userId: Long): Boolean
    fun deleteByIdAndUserUserId(id: Long, userId: Long)
    fun findByIdAndUserUserId(id: Long, userId: Long): Debt?
}
