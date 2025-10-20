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
    val message = mockk<Message>(relaxed = true)
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
        messageProcessors = mapOf(StateCode.START to messageProcessor),
        callbackProcessors = mapOf(StateCode.SETTINGS to callbackProcessor)
    )

    beforeTest {
        messageProcessor.executed = false
        callbackProcessor.executed = false
    }

    should("invoke mapped message processor") {
        context.execute(telegramUser, message, StateCode.START)
        messageProcessor.executed shouldBe true
    }

    should("return nothing when message processor is absent") {
        context.execute(telegramUser, message, StateCode.HELP)
        messageProcessor.executed shouldBe false
    }

    should("invoke callback processor when available") {
        val status = context.execute(telegramUser, "payload", StateCode.SETTINGS)
        callbackProcessor.executed shouldBe true
        status shouldBe ExecuteStatus.FINAL
    }

    should("return ExecuteStatus.NOTHING when callback processor missing") {
         val status = context.execute(telegramUser, "payload", StateCode.MAZE)
         status shouldBe ExecuteStatus.NOTHING
    }
})
