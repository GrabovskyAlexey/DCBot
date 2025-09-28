package ru.grabovsky.dungeoncrusherbot.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.dungeoncrusherbot.entity.Maze
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.entity.VerificationRequest
import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.repository.VerificationRequestRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MazeService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ResourcesService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class VerificationServiceImplTest : ShouldSpec({

    val stateService = mockk<StateService>()
    val verificationRepository = mockk<VerificationRequestRepository>()
    val resourcesService = mockk<ResourcesService>()
    val userService = mockk<UserService>()
    val mazeService = mockk<MazeService>()
    val service = VerificationServiceImpl(stateService, verificationRepository, resourcesService, userService, mazeService)

    val telegramUser = mockk<TgUser>(relaxed = true) {
        every { id } returns 501L
        every { firstName } returns "Tester"
    }

    lateinit var request: VerificationRequest
    lateinit var userState: UserState
    lateinit var dbUser: User

    fun prepareRequest(message: String, code: StateCode) {
        request = VerificationRequest(message = message, stateCode = code)
        userState = UserState(userId = 501L, state = StateCode.VERIFY, verification = request)
        every { stateService.getState(telegramUser) } returns userState
        every { verificationRepository.save(request) } returns request
    }

    beforeTest {
        clearMocks(stateService, verificationRepository, resourcesService, userService, mazeService, answers = true)
        dbUser = User(userId = 501L, firstName = "Tester", lastName = null, userName = "tester")
        justRun { resourcesService.processResources(any(), any(), any()) }
        every { userService.getUser(any()) } returns dbUser
        justRun { userService.processNote(any(), any(), any()) }
        justRun { mazeService.processSameStep(any(), any(), any()) }
    }

    should("do nothing when verification request is missing") {
        val state = UserState(userId = 501L, state = StateCode.VERIFY, verification = null)
        every { stateService.getState(telegramUser) } returns state

        service.verify(telegramUser, StateCode.VERIFY)

        verify(exactly = 0) { verificationRepository.save(any()) }
        verify(exactly = 0) { resourcesService.processResources(any(), any(), any()) }
    }

    should("approve exchange when message is not empty") {
        prepareRequest("exchange", StateCode.ADD_EXCHANGE)

        service.verify(telegramUser, StateCode.ADD_EXCHANGE)

        request.result.shouldBeTrue()
        verify { resourcesService.processResources(telegramUser, "exchange", StateCode.ADD_EXCHANGE) }
        verify { verificationRepository.save(request) }
    }

    should("reject exchange when message is blank") {
        prepareRequest("", StateCode.ADD_EXCHANGE)

        service.verify(telegramUser, StateCode.ADD_EXCHANGE)

        request.result.shouldBeFalse()
        verify(exactly = 0) { resourcesService.processResources(any(), any(), any()) }
        verify { verificationRepository.save(request) }
    }

    should("process resource states when numeric value is valid") {
        prepareRequest("5", StateCode.ADD_DRAADOR)

        service.verify(telegramUser, StateCode.ADD_DRAADOR)

        request.result.shouldBeTrue()
        verify { resourcesService.processResources(telegramUser, "5", StateCode.ADD_DRAADOR) }
    }

    should("not process resource states when numeric value is invalid") {
        prepareRequest("not-a-number", StateCode.SEND_DRAADOR)

        service.verify(telegramUser, StateCode.SEND_DRAADOR)

        request.result.shouldBeFalse()
        verify(exactly = 0) { resourcesService.processResources(any(), any(), any()) }
    }

    should("process remove note when index is valid") {
        dbUser.notes.add("first")
        prepareRequest("1", StateCode.REMOVE_NOTE)

        service.verify(telegramUser, StateCode.REMOVE_NOTE)

        request.result.shouldBeTrue()
        verify { userService.processNote(dbUser, "1", StateCode.REMOVE_NOTE) }
    }

    should("skip remove note when index is invalid") {
        dbUser.notes.add("only")
        prepareRequest("5", StateCode.REMOVE_NOTE)

        service.verify(telegramUser, StateCode.REMOVE_NOTE)

        request.result.shouldBeFalse()
        verify(exactly = 0) { userService.processNote(any(), any(), any()) }
    }

    should("process maze verification when steps are valid") {
        val maze = Maze(user = dbUser)
        dbUser.maze = maze
        prepareRequest("3", StateCode.SAME_LEFT)

        service.verify(telegramUser, StateCode.SAME_LEFT)

        request.result.shouldBeTrue()
        verify { mazeService.processSameStep(maze, Direction.LEFT, 3) }
    }

    should("propagate error when maze verification input is invalid") {
        val maze = Maze(user = dbUser)
        dbUser.maze = maze
        prepareRequest("oops", StateCode.SAME_RIGHT)

        shouldThrow<NumberFormatException> {
            service.verify(telegramUser, StateCode.SAME_RIGHT)
        }

        verify(exactly = 0) { mazeService.processSameStep(any(), any(), any()) }
    }
})

