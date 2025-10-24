package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.dungeoncrusherbot.entity.*
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MazeService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.maze.ConfirmRefreshMazeProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.maze.MazeProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.note.NotesProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.UPDATE_MAZE
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.UPDATE_NOTES
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class CallbackProcessorsTest : ShouldSpec({
    val tgUser = mockk<TgUser>(relaxed = true) { every { id } returns 950L; every { firstName } returns "Tester" }

    should("очищать заметки через NotesProcessor") {
        val stateService = mockk<StateService>()
        val userService = mockk<UserService>()
        val userState = UserState(userId = 950L, state = StateCode.NOTES)
        every { stateService.getState(tgUser) } returns userState
        every { stateService.saveState(userState) } returns userState
        justRun { userService.clearNotes(tgUser) }

        val processor = NotesProcessor(stateService, userService)
        processor.execute(tgUser, "CLEAR_NOTES")

        userState.prevState shouldBe UPDATE_NOTES
        verify { userService.clearNotes(tgUser) }
        verify { stateService.saveState(userState) }
    }

    should("обрабатывать движение по лабиринту через MazeProcessor") {
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

    should("сбрасывать лабиринт при подтверждении") {
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
