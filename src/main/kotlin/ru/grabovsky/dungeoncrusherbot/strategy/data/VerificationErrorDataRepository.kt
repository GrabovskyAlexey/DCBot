package ru.grabovsky.dungeoncrusherbot.strategy.data

import org.springframework.stereotype.Repository
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.VerificationErrorDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.*

@Repository
class VerificationErrorDataRepository(
    private val stateService: StateService
): AbstractDataRepository<VerificationErrorDto>() {
    override fun getData(user: User): VerificationErrorDto {
        val stateCode = stateService.getState(user).verification?.stateCode
        return when (stateCode) {
            SAME_LEFT, SAME_CENTER, SAME_RIGHT -> VerificationErrorDto("Введите количество шагов от 1 до 10")
            else -> VerificationErrorDto("Неопознанная ошибка")
        }
    }
}
