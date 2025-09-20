package ru.grabovsky.dungeoncrusherbot.strategy.data.maze

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.entity.Location
import ru.grabovsky.dungeoncrusherbot.entity.Maze
import ru.grabovsky.dungeoncrusherbot.entity.Step
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class MazeDataRepositoryTest : ShouldSpec({
    val userService = mockk<UserService>()
    val stateService = mockk<StateService>()
    val repository = MazeDataRepository(userService, stateService)

    should("возвращать текущую позицию лабиринта без истории по умолчанию") {
        val entityUser = User(
            userId = 620L,
            firstName = "Maze",
            lastName = null,
            userName = "maze"
        ).apply {
            maze = Maze(user = this).apply {
                currentLocation = Location(level = 3, offset = 1, direction = Direction.LEFT)
                sameSteps = false
            }
        }
        every { userService.getUser(620L) } returns entityUser
        val tgUser = mockk<TgUser>(relaxed = true) { every { id } returns 620L }
        every { stateService.getState(tgUser) } returns UserState(userId = 620L, state = StateCode.MAZE)

        val dto = repository.getData(tgUser)

        dto.location shouldBe entityUser.maze!!.currentLocation
        dto.steps shouldBe null
        dto.sameSteps shouldBe false
    }

    should("возвращать историю ходов при запросе через callback") {
        val entityUser = User(
            userId = 621L,
            firstName = "Maze",
            lastName = null,
            userName = "maze2"
        ).apply {
            maze = Maze(user = this).apply {
                currentLocation = Location(level = 5, offset = 2, direction = Direction.RIGHT)
                sameSteps = true
                steps.addAll(
                    listOf(
                        Step(Direction.LEFT, Location(0, 0, Direction.CENTER), Location(1, 0, Direction.LEFT)),
                        Step(Direction.RIGHT, Location(1, 0, Direction.LEFT), Location(2, 1, Direction.RIGHT))
                    )
                )
            }
        }
        every { userService.getUser(621L) } returns entityUser
        val tgUser = mockk<TgUser>(relaxed = true) { every { id } returns 621L }
        val state = UserState(userId = 621L, state = StateCode.MAZE, callbackData = "HISTORY")
        every { stateService.getState(tgUser) } returns state
        justRun { stateService.saveState(any()) }
        justRun { stateService.updateState(tgUser, any()) }

        val dto = repository.getData(tgUser)

        dto.location shouldBe entityUser.maze!!.currentLocation
        dto.steps shouldContainExactly entityUser.maze!!.steps.map { it.toString() }
        dto.sameSteps shouldBe true
        verify { stateService.updateState(tgUser, StateCode.MAZE) }
    }
})
