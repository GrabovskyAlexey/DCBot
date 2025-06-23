package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.Processor


abstract class CallbackProcessor(
    private val stateService: StateService
): Processor {
    fun execute(user: User, callbackQuery: CallbackQuery): ExecuteStatus {
        val state =
            stateService.getState(user)
        state
            .apply { this.callbackData = callbackQuery.data }
        stateService.saveState(state)
        return process(user, callbackQuery)
    }
    abstract fun process(user: User, callbackQuery: CallbackQuery): ExecuteStatus
}