package ru.grabovsky.dungeoncrusherbot.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.entity.Maze
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.entity.UserProfile
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.entity.VerificationRequest
import ru.grabovsky.dungeoncrusherbot.repository.VerificationRequestRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MazeService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class VerificationServiceImplTest : ShouldSpec({

    val stateService = mockk<StateService>()
    val verificationRepository = mockk<VerificationRequestRepository>()
    val userService = mockk<UserService>()
    val mazeService = mockk<MazeService>()
    val service = VerificationServiceImpl(stateService, verificationRepository, userService, mazeService)

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
        clearMocks(stateService, verificationRepository, userService, mazeService, answers = true)
        dbUser = User(userId = 501L, firstName = "Tester", lastName = null, userName = "tester").apply {
            profile = UserProfile(userId = userId, user = this)
        }
        every { userService.getUser(any()) } returns dbUser
        justRun { mazeService.processSameStep(any(), any(), any()) }
    }

    should("do nothing when verification request is missing") {
         val state = UserState(userId = 501L, state = StateCode.VERIFY, verification = null)
         every { stateService.getState(telegramUser) } returns state

         service.verify(telegramUser, StateCode.VERIFY)

         verify(exactly = 0) { verificationRepository.save(any()) }
         verify(exactly = 0) { mazeService.processSameStep(any(), any(), any()) }
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
