package ru.grabovsky.dungeoncrusherbot.strategy.flow.subscribe

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.context.MessageSource
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.*
import java.util.*
import ru.grabovsky.dungeoncrusherbot.entity.User as BotUser

class SubscribeFlowTest : ShouldSpec({

    val userService = mockk<UserService>(relaxed = true)
    val serverService = mockk<ServerService>()
    val messageSource = mockk<MessageSource>()

    val flow = SubscribeFlow(userService, serverService, messageSource)

    val locale = Locale("ru")
    val telegramUser = mockk<User>(relaxed = true) {
        every { id } returns 42L
        every { firstName } returns "Test"
    }

    val serverOne = Server(id = 1, name = "One")
    val serverTwo = Server(id = 2, name = "Two")

    beforeTest {
        clearMocks(userService, serverService, messageSource)
        every { messageSource.getMessage(any(), any(), any(), any<Locale>()) } answers {
            thirdArg<String?>() ?: firstArg<String>()
        }
        justRun { userService.saveUser(any()) }
    }

    should("render initial message with current subscriptions") {
        val persistedUser = BotUser(
            userId = telegramUser.id,
            firstName = telegramUser.firstName,
            lastName = telegramUser.lastName,
            userName = telegramUser.userName
        ).apply { servers.add(serverOne) }

        every { userService.getUser(any()) } returns persistedUser
        every { serverService.getAllServers() } returns listOf(serverOne, serverTwo)

        val result = flow.start(FlowStartContext(telegramUser, locale))

        result.stepKey shouldBe StepKey.MAIN.key
        result.payload shouldBe Unit
        result.actions shouldHaveSize 1

        val action = result.actions.single() as SendMessageAction
        action.bindingKey shouldBe "subscribe_main"
        action.message.flowKey shouldBe FlowKeys.SUBSCRIBE
        action.message.stepKey shouldBe StepKey.MAIN.key
        val viewModel = action.message.model as SubscribeViewModel
        viewModel.servers shouldBe listOf(1)
        action.message.inlineButtons.shouldNotBeEmpty()
        val payloads = action.message.inlineButtons.map { it.payload.data }
        payloads shouldContain "UNSUBSCRIBE:1"
        payloads shouldContain "SUBSCRIBE:2"
    }

    should("subscribe user and update message on callback") {
        val persistedUser = BotUser(
            userId = telegramUser.id,
            firstName = telegramUser.firstName,
            lastName = telegramUser.lastName,
            userName = telegramUser.userName
        )

        every { userService.getUser(any()) } returns persistedUser
        every { serverService.getAllServers() } returns listOf(serverOne, serverTwo)
        every { serverService.getServerById(2) } returns serverTwo

        val callbackQuery = mockk<CallbackQuery> {
            every { id } returns "cb-id"
            every { from } returns telegramUser
        }

        val context = FlowCallbackContext(
            user = telegramUser,
            locale = locale,
            state = FlowStateHolder(
                stepKey = StepKey.MAIN.key,
                payload = Unit,
                messageBindings = mapOf("subscribe_main" to 100)
            )
        )

        val result = flow.onCallback(context, callbackQuery, "SUBSCRIBE:2")

        verify { userService.saveUser(withArg { it.servers.any { server -> server.id == 2 } shouldBe true }) }

        result!!.actions shouldHaveSize 2
        val editAction = result.actions[0] as EditMessageAction
        editAction.bindingKey shouldBe "subscribe_main"
        val message = editAction.message
        message.flowKey shouldBe FlowKeys.SUBSCRIBE
        (message.model as SubscribeViewModel).servers shouldBe listOf(2)
        val updatedPayloads = message.inlineButtons.map { it.payload.data }
        updatedPayloads shouldContain "SUBSCRIBE:1"
        updatedPayloads shouldContain "UNSUBSCRIBE:2"

        val answerAction = result.actions[1] as AnswerCallbackAction
        answerAction.callbackQueryId shouldBe "cb-id"
    }
})
