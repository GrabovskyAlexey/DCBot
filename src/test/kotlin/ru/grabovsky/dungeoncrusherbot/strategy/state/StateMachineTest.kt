package ru.grabovsky.dungeoncrusherbot.strategy.state

import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.testcontainers.containers.PostgreSQLContainer
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.entity.VerificationRequest
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.state.maze.MazeState
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


    context("VerifyState") {
        val stateService = mockk<StateService>()
        val verifyState = VerifyState(stateService)

        should("go to success when verification result is true") {
            val verification = VerificationRequest(message = "ok", stateCode = StateCode.ADD_EXCHANGE).apply { result = true }
            every { stateService.getState(user) } returns UserState(userId = 101L, state = StateCode.VERIFY, verification = verification)
            verifyState.getNextState(user) shouldBe StateCode.VERIFICATION_SUCCESS
        }

        should("go to error when verification result is false") {
            val verification = VerificationRequest(message = "bad", stateCode = StateCode.ADD_EXCHANGE, result = false)
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

})
