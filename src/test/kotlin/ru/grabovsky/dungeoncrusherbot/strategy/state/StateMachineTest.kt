package ru.grabovsky.dungeoncrusherbot.strategy.state

import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.entity.VerificationRequest
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.context.StateContext
import ru.grabovsky.dungeoncrusherbot.strategy.state.maze.MazeState
import ru.grabovsky.dungeoncrusherbot.strategy.state.settings.SettingsState
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager
import org.telegram.telegrambots.meta.api.objects.User as TgUser

private class KPostgreSQLContainer(imageName: String) :
    PostgreSQLContainer<KPostgreSQLContainer>(imageName)

private object PostgresContainerExtension : BeforeSpecListener, AfterSpecListener {
    private val container = KPostgreSQLContainer("postgres:16-alpine").apply {
        withDatabaseName("dc_bot")
        withUsername("postgres")
        withPassword("postgres")
    }
    private var started = false

    override suspend fun beforeSpec(spec: Spec) {
        if (!started) {
            container.start()
            started = true
        }
    }

    override suspend fun afterSpec(spec: Spec) {
        if (started) {
            container.stop()
            started = false
        }
    }

    fun assertConnection() {
        DriverManager.getConnection(container.jdbcUrl, container.username, container.password).use { conn ->
            conn.createStatement().executeQuery("SELECT 1").use { rs ->
                rs.next() shouldBe true
            }
        }
    }
}

class StateMachineTest : ShouldSpec({
    extension(PostgresContainerExtension)

    val user = mockk<TgUser>(relaxed = true) {
        every { id } returns 101L
        every { firstName } returns "Tester"
    }

    context("database") {
        should("be reachable from tests") {
            PostgresContainerExtension.assertConnection()
        }
    }

    context("StateContext") {
        val ctx = StateContext(listOf(StartState(), HelpState(), SendReportState()))

        should("return next state for known state code") {
            ctx.next(user, StateCode.START) shouldBe StateCode.WAITING
        }

        should("return null for unknown state code") {
            ctx.next(user, StateCode.NOTIFY) shouldBe null
        }
    }

    context("Start and SendReport states") {
        should("lead to WAITING from StartState") {
            StartState().getNextState(user) shouldBe StateCode.WAITING
        }

        should("lead to WAITING from SendReportState") {
            SendReportState().getNextState(user) shouldBe StateCode.WAITING
        }
    }

    context("VerifyState") {
        val stateService = mockk<StateService>()
        val verifyState = VerifyState(stateService)

        should("go to success when verification result is true") {
            val verification = VerificationRequest(message = "ok", stateCode = StateCode.ADD_NOTE).apply { result = true }
            every { stateService.getState(user) } returns UserState(userId = 101L, state = StateCode.VERIFY, verification = verification)
            verifyState.getNextState(user) shouldBe StateCode.VERIFICATION_SUCCESS
        }

        should("go to error when verification result is false") {
            val verification = VerificationRequest(message = "bad", stateCode = StateCode.ADD_NOTE, result = false)
            every { stateService.getState(user) } returns UserState(userId = 101L, state = StateCode.VERIFY, verification = verification)
            verifyState.getNextState(user) shouldBe StateCode.VERIFICATION_ERROR
        }

        should("go to error when verification is missing") {
            every { stateService.getState(user) } returns UserState(userId = 101L, state = StateCode.VERIFY, verification = null)
            verifyState.getNextState(user) shouldBe StateCode.VERIFICATION_ERROR
        }
    }

    context("MazeState") {
        val stateService = mockk<StateService>()
        val mazeState = MazeState(stateService)

        should("return SAME_LEFT when callback requests it") {
            every { stateService.getState(user) } returns UserState(userId = 101L, state = StateCode.MAZE, callbackData = "SAME_LEFT")
            mazeState.getNextState(user) shouldBe StateCode.SAME_LEFT
        }

        should("return CONFIRM_REFRESH_MAZE when callback is REFRESH_MAZE") {
            every { stateService.getState(user) } returns UserState(userId = 101L, state = StateCode.MAZE, callbackData = "REFRESH_MAZE")
            mazeState.getNextState(user) shouldBe StateCode.CONFIRM_REFRESH_MAZE
        }

        should("default to UPDATE_MAZE") {
            every { stateService.getState(user) } returns UserState(userId = 101L, state = StateCode.MAZE, callbackData = "UNKNOWN")
            mazeState.getNextState(user) shouldBe StateCode.UPDATE_MAZE
        }
    }

    context("SettingsState") {
        val stateService = mockk<StateService>()
        val settingsState = SettingsState(stateService)

        should("route SEND_REPORT callback") {
            every { stateService.getState(user) } returns UserState(userId = 101L, state = StateCode.SETTINGS, callbackData = "SEND_REPORT")
            settingsState.getNextState(user) shouldBe StateCode.SEND_REPORT
        }

        should("default to UPDATE_SETTINGS on other callbacks") {
            every { stateService.getState(user) } returns UserState(userId = 101L, state = StateCode.SETTINGS, callbackData = "OTHER")
            settingsState.getNextState(user) shouldBe StateCode.UPDATE_SETTINGS
        }
    }

})
