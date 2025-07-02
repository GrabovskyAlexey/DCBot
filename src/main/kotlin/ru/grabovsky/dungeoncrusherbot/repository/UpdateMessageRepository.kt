package ru.grabovsky.dungeoncrusherbot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.entity.UpdateMessage

@Repository
interface UpdateMessageRepository: JpaRepository<UpdateMessage, Long> {
    fun findUpdateMessagesBySentNot(sent: Boolean = true): List<UpdateMessage>
}