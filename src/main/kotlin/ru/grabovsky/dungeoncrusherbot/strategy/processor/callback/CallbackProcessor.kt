package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.Processor


abstract class CallbackProcessor(
    private val stateService: StateService
): Processor {
    fun execute(user: User, callbackData: String): ExecuteStatus {
        val state =
            stateService.getState(user)
        state
            .apply { this.callbackData = callbackData }
        stateService.saveState(state)
        return process(user, callbackData)
    }
    abstract fun process(user: User, callbackData: String): ExecuteStatus
}