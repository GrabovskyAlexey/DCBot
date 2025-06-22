package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.strategy.processor.Processor


interface CallbackProcessor: Processor {
    fun execute(user: User, callbackQuery: CallbackQuery): ExecuteStatus
}