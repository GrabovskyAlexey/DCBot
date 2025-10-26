package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.entity.Maze
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MazeService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.maze.ConfirmRefreshMazeProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.maze.MazeProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.UPDATE_MAZE
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class CallbackProcessorsTest : ShouldSpec({
    val tgUser = mockk<TgUser>(relaxed = true) { every { id } returns 950L; every { firstName } returns "Tester" }

    should("обновлять данные по движениям в MazeProcessor") {
        val stateService = mockk<StateService>()
        val userService = mockk<UserService>()
        val mazeService = mockk<MazeService>(relaxed = true)
        val userState = UserState(userId = 950L, state = StateCode.MAZE)
        every { stateService.getState(tgUser) } returns userState
        every { stateService.saveState(userState) } returns userState

        val maze = Maze(user = User(950L, "Tester", null, "tester"))
        val entityUser = maze.user!!
        entityUser.maze = maze
        every { userService.getUser(950L) } returns entityUser

        val processor = MazeProcessor(userService, mazeService, stateService)
        processor.execute(tgUser, "LEFT")
        verify { mazeService.processStep(maze, Direction.LEFT) }
        processor.execute(tgUser, "SAME_STEPS")
        verify { mazeService.revertSameSteps(maze) }
        userState.prevState shouldBe UPDATE_MAZE
    }

    should("обрабатывать подтверждение сброса лабиринта") {
        val stateService = mockk<StateService>(relaxed = true)
        val userService = mockk<UserService>()
        val mazeService = mockk<MazeService>(relaxed = true)
        val entityUser = User(951L, "Tester", null, "tester").apply { maze = Maze(user = this) }
        val otherUser = mockk<TgUser>(relaxed = true) { every { id } returns 951L; every { firstName } returns "Tester" }
        every { userService.getUser(951L) } returns entityUser

        val processor = ConfirmRefreshMazeProcessor(userService, mazeService, stateService)
        processor.execute(otherUser, "CONFIRM")
        verify { mazeService.refreshMaze(entityUser.maze!!) }

        processor.execute(otherUser, "NOT_CONFIRM")
        verify(exactly = 1) { mazeService.refreshMaze(any()) }
    }
})

