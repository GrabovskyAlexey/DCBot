package ru.grabovsky.dungeoncrusherbot.strategy.context

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.CallbackProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.processor.callback.ExecuteStatus
import ru.grabovsky.dungeoncrusherbot.strategy.processor.message.MessageProcessor
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message

class LogicContextTest : ShouldSpec({

    val telegramUser = mockk<User>(relaxed = true)
    val stateService = mockk<StateService>(relaxed = true)

    val messageProcessor = object : MessageProcessor {
        var executed = false
        override fun execute(user: User, message: Message) {
            executed = true
        }
    }

    val callbackProcessor = object : CallbackProcessor(stateService) {
        var executed = false
        override fun process(user: User, callbackData: String): ExecuteStatus {
            executed = true
            return ExecuteStatus.FINAL
        }
    }

    val context = LogicContext(
        messageProcessors = mapOf(StateCode.NOTES to messageProcessor),
        callbackProcessors = mapOf(StateCode.NOTES to callbackProcessor)
    )

    beforeTest {
        messageProcessor.executed = false
        callbackProcessor.executed = false
    }

    should("return ExecuteStatus.NOTHING when callback processor missing") {
         val status = context.execute(telegramUser, "payload", StateCode.MAZE)
         status shouldBe ExecuteStatus.NOTHING
    }
})
