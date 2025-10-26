package ru.grabovsky.dungeoncrusherbot.service


import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.entity.Maze
import ru.grabovsky.dungeoncrusherbot.repository.VerificationRequestRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MazeService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.VerificationService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.*

@Service
class VerificationServiceImpl(
    private val stateService: StateService,
    private val verificationRequestRepository: VerificationRequestRepository,
    private val userService: UserService,
    private val mazeService: MazeService,
) : VerificationService {

    override fun verify(user: User, stateCode: StateCode) {
        val state = stateService.getState(user)
        val request = state.verification ?: return
        val verificationResult = runCatching {
            request.result = when (request.stateCode) {
                SAME_LEFT, SAME_RIGHT, SAME_CENTER -> request.message.toInt() in 1..10
                else -> false
            }
            return@runCatching request.result
        }.onFailure { error ->
            request.result = false
            logger.warn { "Failed to verify data. Error ${error.message}" }
        }.getOrDefault(false)
        verificationRequestRepository.save(request)
        when {
            mazeStates.contains(request.stateCode) -> processMaze(user, request.message, request.stateCode)
        }
    }

    private fun processMaze(user: User, value: String, state: StateCode) {
        val steps = value.toInt()
        val userFromDb = userService.getUser(user.id) ?: return
        val maze = userFromDb.maze ?: Maze(user = userFromDb)
        when(state) {
            SAME_LEFT -> mazeService.processSameStep(maze, Direction.LEFT, steps)
            SAME_RIGHT -> mazeService.processSameStep(maze, Direction.RIGHT, steps)
            SAME_CENTER -> mazeService.processSameStep(maze, Direction.CENTER, steps)
            else -> {}
        }
    }

    companion object {
        val logger = KotlinLogging.logger {}
        val mazeStates = setOf(SAME_LEFT, SAME_CENTER, SAME_RIGHT)
    }
}
