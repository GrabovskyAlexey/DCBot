package ru.grabovsky.dungeoncrusherbot.service.interfaces

import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.entity.Maze

interface MazeService {
    fun getMaze()
    fun processStep(maze: Maze, direction: Direction)
    fun refreshMaze(maze: Maze)
}