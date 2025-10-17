package ru.grabovsky.dungeoncrusherbot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.entity.CallbackData
import ru.grabovsky.dungeoncrusherbot.entity.CallbackDataType

@Repository
interface CallbackDataRepository: JpaRepository<CallbackData, Long> {
    fun findByTypeAndUserId(type: CallbackDataType, userId: Long): CallbackData?
}