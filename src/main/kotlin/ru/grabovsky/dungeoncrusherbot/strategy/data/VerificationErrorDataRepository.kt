package ru.grabovsky.dungeoncrusherbot.strategy.data

import org.springframework.stereotype.Repository
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.VerificationErrorDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.*

@Repository
class VerificationErrorDataRepository(
    private val userService: UserService,
    private val stateService: StateService
): AbstractDataRepository<VerificationErrorDto>() {
    override fun getData(user: User): VerificationErrorDto {
        val stateCode = stateService.getState(user).verification?.stateCode
        return when(stateCode) {
            ADD_DRAADOR, SELL_DRAADOR, SEND_DRAADOR, RECEIVE_DRAADOR, ADD_VOID, REMOVE_VOID, ADD_CB, REMOVE_CB -> VerificationErrorDto("Введите положительное число")
            ADD_EXCHANGE -> VerificationErrorDto("Введите корректный ник")
            ADD_NOTE -> VerificationErrorDto("Заметка должна содержать только текст")
            REMOVE_NOTE -> VerificationErrorDto("Введите корректный номер заметки для удаления")
            SAME_LEFT, SAME_CENTER, SAME_RIGHT -> VerificationErrorDto("Введите количество шагов от 1 до 10")
            else -> VerificationErrorDto("Неопознанная ошибка")
        }
    }
}