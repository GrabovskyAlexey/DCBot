package ru.grabovsky.dungeoncrusherbot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.entity.Direction.*
import ru.grabovsky.dungeoncrusherbot.entity.Location
import ru.grabovsky.dungeoncrusherbot.entity.Maze
import ru.grabovsky.dungeoncrusherbot.entity.Step
import ru.grabovsky.dungeoncrusherbot.repository.MazeRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MazeService
import kotlin.system.measureTimeMillis

@Service
class MazeServiceImpl(
    private val mazeRepository: MazeRepository
) : MazeService {

    override fun processStep(maze: Maze, direction: Direction) {
        measureTimeMillis {
            val currentLocation = maze.currentLocation ?: Location(0, 0, CENTER)
            if (currentLocation.level >= 500) return
            processStep(direction, currentLocation, maze)
            mazeRepository.saveAndFlush(maze)
        }.also { logger.info { "Maze process complete: ${it}ms" } }
    }

    private fun processStep(
        direction: Direction,
        currentLocation: Location,
        maze: Maze
    ): Location {
        val nextLocation = when (direction) {
            LEFT -> walkLeft(currentLocation)
            RIGHT -> walkRight(currentLocation)
            CENTER -> Location(currentLocation.level + 1, currentLocation.offset, currentLocation.direction)
        }
        updateHistory(
            maze.steps,
            Step(
                direction = direction,
                startLocation = currentLocation,
                finishLocation = nextLocation
            )
        )
        maze.currentLocation = nextLocation
        return nextLocation
    }

    override fun processSameStep(maze: Maze, direction: Direction, steps: Int) {
        measureTimeMillis {
            var currentLocation = maze.currentLocation ?: Location(0, 0, CENTER)
            if (currentLocation.level >= 500) return
            for(i in 0 until steps) {
                currentLocation = processStep(direction, currentLocation, maze)
                if (currentLocation.level >= 500) break
            }
            mazeRepository.saveAndFlush(maze)
        }.also { logger.info { "Maze process complete: ${it}ms" } }
    }

    override fun refreshMaze(maze: Maze) {
        maze.currentLocation = Location(0, 0, CENTER)
        maze.steps.clear()
        mazeRepository.saveAndFlush(maze)
    }

    private fun walkLeft(startLocation: Location): Location {
        val nextLevel = startLocation.level + 1
        var offset = when (startLocation.direction) {
            LEFT, CENTER -> startLocation.offset + 1
            RIGHT -> startLocation.offset - 1
        }
        val direction = when (startLocation.direction) {
            RIGHT -> if (offset == 0) CENTER else RIGHT
            CENTER -> LEFT
            LEFT -> {
                if (offset > 7) {
                    offset = 7
                    RIGHT
                } else LEFT
            }
        }
        return Location(nextLevel, offset, direction)
    }

    private fun walkRight(startLocation: Location): Location {
        val nextLevel = startLocation.level + 1
        var offset = when (startLocation.direction) {
            RIGHT, CENTER -> startLocation.offset + 1
            LEFT -> startLocation.offset - 1
        }
        val direction = when (startLocation.direction) {
            LEFT -> if (offset == 0) CENTER else LEFT
            CENTER -> RIGHT
            RIGHT -> {
                if (offset > 7) {
                    offset = 7
                    LEFT
                } else RIGHT
            }
        }
        return Location(nextLevel, offset, direction)
    }

    override fun revertSameSteps(maze: Maze) {
        maze.sameSteps = !maze.sameSteps
        mazeRepository.saveAndFlush(maze)
    }

    private fun updateHistory(history: MutableList<Step>, item: Step) {
        while (history.size >= 20) {
            history.removeFirst()
        }
        history.addLast(item)
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}