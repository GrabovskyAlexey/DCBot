package ru.grabovsky.dungeoncrusherbot.service

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.framework.exception.StateNotFoundException
import ru.grabovsky.dungeoncrusherbot.repository.StateRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

@Service
class StateServiceImpl(
    private val stateRepository: StateRepository
) : StateService {
    override fun updateState(
        user: User,
        code: StateCode,
        callbackData: String?
    ) {
        val state = stateRepository.findByUserId(user.id) ?: UserState(
            userId = user.id,
            state = code
        )
        state.apply {
            this.state = code
        }.also { stateRepository.saveAndFlush(it) }
    }

    override fun getState(user: User): UserState =
        stateRepository.findByUserId(user.id)
            ?: throw StateNotFoundException("State for user: ${user.id} not found")

    override fun saveState(state: UserState) = stateRepository.saveAndFlush(state)
}