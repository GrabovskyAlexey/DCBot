package ru.grabovsky.dungeoncrusherbot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import ru.grabovsky.dungeoncrusherbot.entity.Maze

@Repository
interface MazeRepository: JpaRepository<Maze, Long> {
    fun findMazesById(id: Long): Maze?

    @Modifying
    @Transactional
    @Query("delete from Maze m where m.user.userId = :userId")
    fun deleteAllByUserId(@Param("userId") userId: Long): Int
}
