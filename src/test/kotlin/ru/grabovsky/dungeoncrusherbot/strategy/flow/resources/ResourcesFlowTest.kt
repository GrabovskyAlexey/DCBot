package ru.grabovsky.dungeoncrusherbot.strategy.flow.resources

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.springframework.context.MessageSource
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.service.interfaces.AdjustType
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ResourceOperation
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ResourcesService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerResourceDto
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowCallbackContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowMessageContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStartContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStateHolder
import java.util.Locale
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class ResourcesFlowTest : ShouldSpec({

    val userService = mockk<UserService>()
    val resourcesService = mockk<ResourcesService>(relaxed = true)
    val viewService = mockk<ResourcesViewService>()
    val promptBuilder = mockk<ResourcesPromptBuilder>()
    val i18nService = mockk<I18nService>()
    val flow = ResourcesFlow(userService, resourcesService, viewService, promptBuilder, i18nService)

    val telegramUser = mockk<TgUser>(relaxed = true) {
        every { id } returns 101L
        every { firstName } returns "Tester"
        every { languageCode } returns "en"
    }
    val locale = Locale.ENGLISH

    val overview = ResourcesOverviewModel(
        summaries = listOf(
        OverviewSummary(
            id = 5,
            statusIcon = "✅",
            main = false,
            exchange = null,
            draadorCount = 0,
            balanceLabel = "",
            voidCount = 0,
            cbEnabled = false,
            cbCount = 0
        )
    ),
        buttons = listOf(Button(action = "5", label = "5", row = 0, col = 0))
    )

    val serverDetail = ServerDetail(
        dto = ServerResourceDto(
            id = 5,
            draadorCount = 0,
            voidCount = 0,
            balance = 0,
            exchange = null,
            history = null,
            hasHistory = false,
            notifyDisable = false,
            main = false,
            cbEnabled = false,
            quickResourceEnabled = true,
            cbCount = 0,
            notes = emptyList(),
            hasMain = false,
        ),
        buttons = listOf(
            Button(label = "Add", action = "PROMPT_ADD_VOID", row = 4, col = 1),
            Button(label = "Back", action = "BACK", row = 99, col = 0)
        )
    )

    beforeTest {
        clearMocks(userService, resourcesService, viewService, promptBuilder, i18nService)
        justRun { viewService.ensureResources(any()) }
        every { viewService.buildOverview(any(), any()) } returns overview
        every { viewService.buildServer(any(), any(), any(), any()) } returns serverDetail
        every { promptBuilder.amountPrompt(any(), any(), invalid = any()) } returns ResourcesPromptModel("enter amount")
        every { i18nService.i18n(any(), any(), any(), any()) } returns "Cancel"
    }

    should("create main overview message on start") {
        val result = flow.start(FlowStartContext(telegramUser, locale))

        result.stepKey shouldBe ResourcesStep.MAIN.key
        result.payload shouldBe ResourcesFlowState()
        result.actions.shouldHaveSize(1)
    }

    should("show server details on callback") {
        val context = FlowCallbackContext(
            user = telegramUser,
            locale = locale,
            state = FlowStateHolder(
                stepKey = ResourcesStep.MAIN.key,
                payload = ResourcesFlowState(),
                messageBindings = mapOf("resources_main_message" to 10)
            )
        )

        val callback = mockk<CallbackQuery>(relaxed = true) { every { id } returns "cb" }
        val result = flow.onCallback(context, callback, "SERVER:5")

        result!!.stepKey shouldBe ResourcesStep.SERVER.key
        result.payload.selectedServerId shouldBe 5
        verify { viewService.buildServer(telegramUser, 5, includeHistory = false, locale = locale) }
    }

    should("send prompt and handle amount input") {
        val promptContext = FlowCallbackContext(
            user = telegramUser,
            locale = locale,
            state = FlowStateHolder(
                stepKey = ResourcesStep.SERVER.key,
                payload = ResourcesFlowState(selectedServerId = 5),
                messageBindings = mapOf("resources_main_message" to 10)
            )
        )

        val promptCallback = mockk<CallbackQuery>(relaxed = true) { every { id } returns "cb" }
        val promptResult = flow.onCallback(promptContext, promptCallback, "ACTION:PROMPT_ADD_VOID")
        promptResult!!.payload.resourcesPendingAction shouldBe ResourcesPendingAction.Amount(AmountActionType.ADD_VOID, 5)

        val messageContext = FlowMessageContext(
            user = telegramUser,
            locale = locale,
            state = FlowStateHolder(
                stepKey = ResourcesStep.SERVER.key,
                payload = promptResult.payload,
                messageBindings = mapOf(
                    "resources_main_message" to 10,
                    "resources_prompt_message" to 11
                )
            )
        )
        val message = mockk<Message>(relaxed = true) { every { text } returns "3" }

        val afterMessage = flow.onMessage(messageContext, message)
        afterMessage!!.payload.resourcesPendingAction shouldBe null
        verify { resourcesService.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.ADD_VOID, 3)) }
    }
})
