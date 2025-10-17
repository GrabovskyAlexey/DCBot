package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.resources

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.CallbackProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.ExecuteStatus
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Component
class ResourcesProcessor(
    stateService: StateService
): CallbackProcessor(stateService) {
    override fun process(
        user: TgUser,
        callbackData: String
    ): ExecuteStatus {
        val data = callbackData.split(" ")

        val state = stateService.getState(user)
        when(data[0]) {
            "RESOURCE" -> state.lastServerIdByState[StateCode.RESOURCES] = data[1].toInt()
        }
        stateService.saveState(state)
        return ExecuteStatus.FINAL
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}