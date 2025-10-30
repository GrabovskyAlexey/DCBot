package ru.grabovsky.dungeoncrusherbot.strategy.processor.callback

import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.strategy.processor.Processor


abstract class CallbackProcessor(
): Processor {
    fun execute(user: User, callbackData: String): ExecuteStatus =
        process(user, callbackData)
    abstract fun process(user: User, callbackData: String): ExecuteStatus
}
