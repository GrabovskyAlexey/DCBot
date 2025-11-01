package ru.grabovsky.dungeoncrusherbot.service

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.dungeoncrusherbot.entity.*
import ru.grabovsky.dungeoncrusherbot.repository.MazeRepository

class MazeServiceImplTest : ShouldSpec({

    val mazeRepository = mockk<MazeRepository>()
    val service = MazeServiceImpl(mazeRepository)
    lateinit var user: User
    lateinit var maze: Maze

    beforeTest {
        clearMocks(mazeRepository)
        user = User(userId = 300L, firstName = "MazeRunner", lastName = null, userName = "runner").apply {
            profile = UserProfile(userId = userId, user = this)
        }
        maze = Maze(user = user)
        user.maze = maze
        every { mazeRepository.saveAndFlush(any()) } answers { firstArg() }
    }

    should("advance to the next location and record step") {
        service.processStep(maze, Direction.CENTER)

        maze.currentLocation shouldBe Location(level = 1, offset = 0, direction = Direction.CENTER)
        maze.steps.shouldHaveSize(1)
        maze.steps.last().direction shouldBe Direction.CENTER
        verify { mazeRepository.saveAndFlush(maze) }
    }

    should("stop processing when maze level limit is reached") {
        maze.currentLocation = Location(level = 500, offset = 0, direction = Direction.CENTER)

        service.processStep(maze, Direction.LEFT)

        verify(exactly = 0) { mazeRepository.saveAndFlush(any()) }
    }

    should("perform multiple same steps until limit or count reached") {
        service.processSameStep(maze, Direction.LEFT, 3)

        maze.steps.shouldHaveSize(3)
        maze.currentLocation?.level shouldBe 3
        maze.currentLocation?.direction shouldBe Direction.LEFT
        verify { mazeRepository.saveAndFlush(maze) }
    }

    should("stop same step processing when level exceeds 500") {
        maze.currentLocation = Location(level = 499, offset = 0, direction = Direction.CENTER)

        service.processSameStep(maze, Direction.RIGHT, 5)

        maze.currentLocation?.level shouldBe 500
        verify { mazeRepository.saveAndFlush(maze) }
    }

    should("refresh maze to initial state") {
        maze.currentLocation = Location(level = 10, offset = 2, direction = Direction.RIGHT)
        maze.steps.add(Step(Direction.RIGHT, Location(9, 1, Direction.RIGHT), Location(10, 2, Direction.RIGHT)))

        service.refreshMaze(maze)

        maze.currentLocation shouldBe Location(level = 0, offset = 0, direction = Direction.CENTER)
        maze.steps.shouldHaveSize(0)
        verify { mazeRepository.saveAndFlush(maze) }
    }

    should("toggle same steps flag") {
        maze.sameSteps shouldBe false

        service.revertSameSteps(maze)
        maze.sameSteps shouldBe true
        service.revertSameSteps(maze)
        maze.sameSteps shouldBe false
        verify(exactly = 2) { mazeRepository.saveAndFlush(maze) }
    }

    should("trim history to last 20 entries") {
        maze.currentLocation = Location(level = 19, offset = 0, direction = Direction.CENTER)
        repeat(20) {
            maze.steps.add(
                Step(
                    Direction.CENTER,
                    Location(it, 0, Direction.CENTER),
                    Location(it + 1, 0, Direction.CENTER)
                )
            )
        }

        service.processStep(maze, Direction.CENTER)

        maze.steps.shouldHaveSize(20)
        maze.steps.first().startLocation.level shouldBe 1
        verify { mazeRepository.saveAndFlush(maze) }
    }
})
