package ru.grabovsky.dungeoncrusherbot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.entity.Maze

@Repository
interface MazeRepository: JpaRepository<Maze, Long> {
    fun findMazesById(id: Long): Maze?
}