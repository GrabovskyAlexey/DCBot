package ru.grabovsky.dungeoncrusherbot.service


import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.repository.VerificationRequestRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ResourcesService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.VerificationService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.*

@Service
class VerificationServiceImpl(
    private val stateService: StateService,
    private val verificationRequestRepository: VerificationRequestRepository,
    private val resourcesService: ResourcesService,
    private val userService: UserService
) : VerificationService {

    override fun verify(user: User, stateCode: StateCode) {
        val state = stateService.getState(user)
        val request = state.verification ?: return
        val verificationResult = runCatching {
            request.result = when (request.stateCode) {
                ADD_EXCHANGE -> request.message.isNotEmpty()
                ADD_VOID, REMOVE_VOID, ADD_DRAADOR, SEND_DRAADOR, RECEIVE_DRAADOR, SELL_DRAADOR, ADD_CB, REMOVE_CB -> request.message.toInt() > 0
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
        if (result) {
            resourcesService.processResources(user, value, state)
        }
    }
    private fun processNote(user: User, value: String, state: StateCode, result: Boolean) {
        val userFromDb = userService.getUser(user.id) ?: return
        if (result) {
            userService.processNote(userFromDb, value, state)
        }
    }

    companion object {
        val logger = KotlinLogging.logger {}
        val resourceStates =
            setOf(ADD_VOID, REMOVE_VOID, ADD_DRAADOR, SEND_DRAADOR, RECEIVE_DRAADOR, SELL_DRAADOR, ADD_EXCHANGE, ADD_CB, REMOVE_CB)
        val noteStates = setOf(ADD_NOTE, REMOVE_NOTE)
    }
}