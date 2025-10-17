package ru.grabovsky.dungeoncrusherbot.service


import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.entity.Maze
import ru.grabovsky.dungeoncrusherbot.repository.VerificationRequestRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.*
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.*

@Service
class VerificationServiceImpl(
    private val stateService: StateService,
    private val verificationRequestRepository: VerificationRequestRepository,
    private val resourcesService: ResourcesService,
    private val userService: UserService,
    private val mazeService: MazeService,
//    private val exchangeRequestService: ExchangeRequestService
) : VerificationService {

    override fun verify(user: User, stateCode: StateCode) {
        val state = stateService.getState(user)
        val request = state.verification ?: return
        val verificationResult = runCatching {
            request.result = when (request.stateCode) {
                ADD_EXCHANGE -> request.message.isNotEmpty()
                ADD_VOID, REMOVE_VOID, ADD_DRAADOR, SEND_DRAADOR, RECEIVE_DRAADOR, SELL_DRAADOR, SET_SOURCE_PRICE, SET_TARGET_PRICE, ADD_CB, REMOVE_CB -> request.message.toInt() > 0
                SAME_LEFT, SAME_RIGHT, SAME_CENTER -> request.message.toInt() in 1..10
                ADD_NOTE -> request.message.isNotEmpty()
                REMOVE_NOTE -> verifyRemoveNotes(user, request.message)
                else -> false
            }
            return@runCatching request.result
        }.onFailure { error ->
            request.result = false
            logger.warn { "Failed to verify data. Error ${error.message}" }
        }.getOrDefault(false)
        verificationRequestRepository.save(request)
        when {
            resourceStates.contains(request.stateCode) -> processResource(user, request.message, request.stateCode, verificationResult)
            noteStates.contains(request.stateCode) -> processNote(user, request.message, request.stateCode, verificationResult)
            mazeStates.contains(request.stateCode) -> processMaze(user, request.message, request.stateCode)
//            exchangeState.contains(request.stateCode) -> exchangeRequestService.processPrice(user, request.message, request.stateCode)
        }
    }

    private fun verifyRemoveNotes(user: User, message: String): Boolean {
        val user = userService.getUser(user.id) ?: return false
        return runCatching {
            val id = message.toInt() - 1
            if (id < 0) return false
            if (user.notes.size <= id) return false
        }.isSuccess
    }

    private fun processResource(user: User, value: String, state: StateCode, result: Boolean) {
        if (!result) {
            return
        }
        resourcesService.processResources(user, value, state)
    }

    private fun processNote(user: User, value: String, state: StateCode, result: Boolean) {
        val userFromDb = userService.getUser(user.id) ?: return
        if (result) {
            userService.processNote(userFromDb, value, state)
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
        val resourceStates =
            setOf(ADD_VOID, REMOVE_VOID, ADD_DRAADOR, SEND_DRAADOR, RECEIVE_DRAADOR, SELL_DRAADOR, ADD_EXCHANGE, ADD_CB, REMOVE_CB)
        val exchangeState = setOf(SET_SOURCE_PRICE, SET_TARGET_PRICE)
        val noteStates = setOf(ADD_NOTE, REMOVE_NOTE)
        val mazeStates = setOf(SAME_LEFT, SAME_CENTER, SAME_RIGHT)
    }
}