package ru.grabovsky.dungeoncrusherbot.service


import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.repository.VerificationRequestRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ResourcesService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.VerificationService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.*

@Service
class VerificationServiceImpl(
    private val stateService: StateService,
    private val verificationRequestRepository: VerificationRequestRepository,
    private val resourcesService: ResourcesService
) : VerificationService {

    override fun verify(user: User, stateCode: StateCode) {
        val request = stateService.getState(user).verification ?: return
        val verificationResult = runCatching {
            request.result = when (request.stateCode) {
                ADD_EXCHANGE -> request.message.isNotEmpty()
                ADD_VOID, REMOVE_VOID, ADD_DRAADOR, SEND_DRAADOR, RECEIVE_DRAADOR, SELL_DRAADOR -> request.message.toInt() > 0
                else -> false
            }
            return@runCatching request.result
        }.onFailure { error ->
            request.result = false
            logger.warn { "Failed to verify data. Error ${error.message}" }
        }.getOrDefault(false)
        verificationRequestRepository.save(request)
        if (verificationResult) {
            resourcesService.processResources(user, request.message, request.stateCode)
        }
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}