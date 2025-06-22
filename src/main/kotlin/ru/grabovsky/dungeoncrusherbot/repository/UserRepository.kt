package ru.grabovsky.dungeoncrusherbot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.entity.User

@Repository
interface UserRepository: JpaRepository<User, Long> {
    fun findUserByUserId(userId: Long): User?
}
