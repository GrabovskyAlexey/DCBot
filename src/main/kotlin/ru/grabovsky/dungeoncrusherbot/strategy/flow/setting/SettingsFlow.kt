package ru.grabovsky.dungeoncrusherbot.strategy.flow.setting

import java.util.Locale
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.entity.NotificationSubscribe
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.AnswerCallbackAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.EditMessageAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowCallbackContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowHandler
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowInlineButton
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKey
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowMessage
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowMessageContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowResult
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStartContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStep
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.SendMessageAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowCallbackPayload
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.PromptState
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.buildMessage
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cleanupPromptMessages
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.cancelPrompt
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.finalizePrompt
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.startPrompt

@Component
class SettingsFlow(
    private val userService: UserService,
    private val i18nService: I18nService,
) : FlowHandler<SettingFlowState> {

    override val key: FlowKey = FlowKeys.SETTINGS
    override val payloadType: Class<SettingFlowState> = SettingFlowState::class.java

    override fun start(context: FlowStartContext): FlowResult<SettingFlowState> {
        val user = userService.getUser(context.user.id)
        return FlowResult(
            stepKey = SettingStepKey.MAIN.key,
            payload = SettingFlowState(),
            actions = listOf(
                SendMessageAction(
                    bindingKey = MAIN_MESSAGE_BINDING,
                    message = buildMessage(user, context.locale)
                )
            )
        )
    }

    override fun onMessage(
        context: FlowMessageContext<SettingFlowState>,
        message: Message
    ): FlowResult<SettingFlowState>? {
        val pending = context.state.payload.pendingAction ?: return null
        if (!pending.sendReport) return null

        userService.sendAdminMessage(context.user, message.text, message.messageId, message.chatId)

        return context.finalizePrompt(
            targetStep = SettingStepKey.SEND_REPORT,
            userMessageId = null,
            updateState = { pendingAction = null }
        ) { }
    }

    override fun onCallback(
        context: FlowCallbackContext<SettingFlowState>,
        callbackQuery: CallbackQuery,
        data: String
    ): FlowResult<SettingFlowState>? {
        val user = userService.getUser(context.user.id) ?: return FlowResult(
            stepKey = context.state.stepKey,
            payload = SettingFlowState(),
            actions = listOf(AnswerCallbackAction(callbackQuery.id))
        )

        val profile = user.profile ?: return FlowResult(
            stepKey = SettingStepKey.MAIN.key,
            payload = SettingFlowState(),
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_BINDING,
                    message = buildMessage(user, context.locale)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )

        when (data) {
            "NOTIFY_SIEGE" -> processNotify(user, NotificationType.SIEGE)
            "NOTIFY_MINE" -> processNotify(user, NotificationType.MINE)
            "CB_ENABLE" -> {
                val settings = profile.settings
                settings.resourcesCb = !settings.resourcesCb
            }
            "QUICK_RESOURCES" -> {
                val settings = profile.settings
                settings.resourcesQuickChange = !settings.resourcesQuickChange
            }
            "SEND_REPORT" -> return startSendReportPrompt(context, callbackQuery)
            "SEND_REPORT_CANCEL" -> return cancelSendReportPrompt(context, callbackQuery)
        }

        userService.saveUser(user)
        return FlowResult(
            stepKey = SettingStepKey.MAIN.key,
            payload = SettingFlowState(),
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_BINDING,
                    message = buildMessage(user, context.locale)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun startSendReportPrompt(
        context: FlowCallbackContext<SettingFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<SettingFlowState> {
        val promptModel = SendReportModel(
            i18nService.i18n("flow.settings.send_report", context.locale)
        )
        val cleanup = context.state.payload.cleanupPromptMessages()
        return context.startPrompt(
            targetStep = SettingStepKey.SEND_REPORT,
            bindingPrefix = PROMPT_MESSAGE_BINDING,
            callbackQuery = callbackQuery,
            updateState = { pendingAction = SettingPendingAction(sendReport = true) },
            appendActions = { addAll(cleanup) }
        ) {
            key.buildMessage(
                step = SettingStepKey.SEND_REPORT,
                model = promptModel,
                inlineButtons = listOf(
                    FlowInlineButton(
                        text = i18nService.i18n("flow.button.cancel", context.locale),
                        payload = FlowCallbackPayload(key.value, "SEND_REPORT_CANCEL")
                    )
                )
            )
        }
    }

    private fun cancelSendReportPrompt(
        context: FlowCallbackContext<SettingFlowState>,
        callbackQuery: CallbackQuery
    ): FlowResult<SettingFlowState> =
        context.cancelPrompt(
            targetStep = SettingStepKey.MAIN,
            callbackQuery = callbackQuery,
            updateState = { pendingAction = null }
        )

    private fun processNotify(user: User, type: NotificationType) {
        val notify = user.notificationSubscribe.firstOrNull { it.type == type }
        if (notify != null) {
            notify.enabled = !notify.enabled
        } else {
            user.notificationSubscribe.add(
                NotificationSubscribe(
                    user = user,
                    type = type,
                    enabled = true
                )
            )
        }
    }

    private fun buildMessage(user: User?, locale: Locale): FlowMessage {
        val profile = user?.profile
        val model = SettingsViewModel(
            siegeEnabled = user?.notificationSubscribe
                ?.firstOrNull { it.type == NotificationType.SIEGE }
                ?.enabled == true,
            mineEnabled = user?.notificationSubscribe
                ?.firstOrNull { it.type == NotificationType.MINE }
                ?.enabled == true,
            cbEnabled = profile?.settings?.resourcesCb ?: false,
            quickResourceEnabled = profile?.settings?.resourcesQuickChange ?: false
        )
        return key.buildMessage(
            step = SettingStepKey.MAIN,
            model = model,
            inlineButtons = buildButtons(model, locale)
        )
    }

    private fun buildButtons(
        model: SettingsViewModel,
        locale: Locale
    ): List<FlowInlineButton> =
        buildList {
            TOGGLE_DEFINITIONS.forEach { definition ->
                val enabled = definition.isEnabled(model)
                val text = i18nService.i18n(
                    code = if (enabled) definition.enabledCode else definition.disabledCode,
                    locale = locale,
                    default = if (enabled) definition.enabledDefault else definition.disabledDefault
                )
                add(buildButton(text, definition.payload, definition.row))
            }
            add(
                buildButton(
                    i18nService.i18n(
                        code = "buttons.settings.send_report",
                        locale = locale,
                        default = "\u270D\uFE0F Отправить пожелание/сообщение об ошибке"
                    ),
                    "SEND_REPORT",
                    99
                )
            )
        }

    private fun buildButton(text: String, data: String, row: Int) =
        FlowInlineButton(
            text = text,
            payload = FlowCallbackPayload(
                flow = key.value,
                data = data
            ),
            row = row
        )

    private data class ToggleDefinition(
        val row: Int,
        val payload: String,
        val isEnabled: (SettingsViewModel) -> Boolean,
        val enabledCode: String,
        val disabledCode: String,
        val enabledDefault: String,
        val disabledDefault: String,
    )

    companion object {
        private const val MAIN_MESSAGE_BINDING = "settings_main"
        private const val PROMPT_MESSAGE_BINDING = "settings_send_report_prompt"
        private val TOGGLE_DEFINITIONS = listOf(
            ToggleDefinition(
                row = 1,
                payload = "NOTIFY_SIEGE",
                isEnabled = { it.siegeEnabled },
                enabledCode = "buttons.settings.siege.enabled",
                disabledCode = "buttons.settings.siege.disabled",
                enabledDefault = "\uD83D\uDCF4 Включить в момент осады",
                disabledDefault = "\uD83D\uDCF4 Включить за 5 минут до осады"
            ),
            ToggleDefinition(
                row = 2,
                payload = "NOTIFY_MINE",
                isEnabled = { it.mineEnabled },
                enabledCode = "buttons.settings.mine.disable",
                disabledCode = "buttons.settings.mine.enable",
                enabledDefault = "\u274C Отключить КШ",
                disabledDefault = "\u2705 Включить КШ"
            ),
            ToggleDefinition(
                row = 3,
                payload = "CB_ENABLE",
                isEnabled = { it.cbEnabled },
                enabledCode = "buttons.settings.cb.disable",
                disabledCode = "buttons.settings.cb.enable",
                enabledDefault = "\u274C Отключить учет КБ",
                disabledDefault = "\u2705 Включить учет КБ"
            ),
            ToggleDefinition(
                row = 4,
                payload = "QUICK_RESOURCES",
                isEnabled = { it.quickResourceEnabled },
                enabledCode = "buttons.settings.quick.disable",
                disabledCode = "buttons.settings.quick.enable",
                enabledDefault = "\u274C Отключить быстрый учет",
                disabledDefault = "\u2705 Включить быстрый учет"
            )
        )
    }
}

data class SettingsViewModel(
    val siegeEnabled: Boolean,
    val mineEnabled: Boolean,
    val cbEnabled: Boolean,
    val quickResourceEnabled: Boolean,
)

data class SendReportModel(val text: String)

data class SettingFlowState(
    var pendingAction: SettingPendingAction? = null,
    override val promptBindings: MutableList<String> = mutableListOf(),
) : PromptState

data class SettingPendingAction(var sendReport: Boolean = true)

enum class SettingStepKey(override val key: String) : FlowStep {
    MAIN("main"),
    SEND_REPORT("send_report")
}
