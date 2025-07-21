package ru.grabovsky.dungeoncrusherbot.service.interfaces

import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.entity.Maze

interface MazeService {
    fun processStep(maze: Maze, direction: Direction)
    fun processSameStep(maze: Maze, direction: Direction, steps: Int)
    fun refreshMaze(maze: Maze)
    fun revertSameSteps(maze: Maze)
}