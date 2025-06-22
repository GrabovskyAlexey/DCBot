package ru.grabovsky.dungeoncrusherbot.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.grabovsky.dungeoncrusherbot.entity.UserState

interface StateRepository: JpaRepository<UserState, Long> {
    fun findByUserId(userId: Long): UserState?
}