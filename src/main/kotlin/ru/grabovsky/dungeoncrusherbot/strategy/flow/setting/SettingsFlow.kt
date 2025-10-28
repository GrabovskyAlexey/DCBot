package ru.grabovsky.dungeoncrusherbot.strategy.flow.setting

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.entity.NotificationSubscribe
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.*
import ru.grabovsky.dungeoncrusherbot.strategy.flow.setting.SettingsType.*
import ru.grabovsky.dungeoncrusherbot.util.FlowUtils
import java.util.*

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
            stepKey = StepKey.MAIN.key,
            payload = SettingFlowState(),
            actions = listOf(
                SendMessageAction(
                    bindingKey = MAIN_MESSAGE_BINDING,
                    message = buildMessage(user, context.locale),
                )
            )
        )
    }

    override fun onMessage(context: FlowMessageContext<SettingFlowState>, message: Message): FlowResult<SettingFlowState>? {
        val pending = context.state.payload.pendingAction ?: return null
        if (!pending.sendReport) return null
        userService.sendAdminMessage(context.user, message.text)
        val state = context.state.payload
        val actions = FlowUtils.cleanupPromptActions(state.promptBindings)
        state.promptBindings.clear()
        state.pendingAction = null

        actions += SendMessageAction(
            bindingKey = MAIN_MESSAGE_BINDING,
            message = FlowMessage(
                flowKey = key,
                stepKey = StepKey.SEND_REPORT.key,
                model = SendReportModel(i18nService.i18n("flow.settings.send_report_complete", context.locale))
            )
        )
        return FlowResult(
            stepKey = StepKey.SEND_REPORT.key,
            payload = state,
            actions = actions,
        )
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
        val profile = user.profile
        if (profile == null) {
            return FlowResult(
                stepKey = StepKey.MAIN.key,
                payload = SettingFlowState(),
                actions = listOf(
                    EditMessageAction(
                        bindingKey = MAIN_MESSAGE_BINDING,
                        message = buildMessage(user, context.locale),
                    ),
                    AnswerCallbackAction(callbackQuery.id),
                )
            )
        }
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
            "SEND_REPORT" -> return buildSendReportMessage(callbackQuery, context.state.payload, context.locale)
            "SEND_REPORT_CANCEL" -> return buildSendReportCancelMessage(callbackQuery, context.state.payload)
        }
        userService.saveUser(user)
        return FlowResult(
            stepKey = StepKey.MAIN.key,
            payload = SettingFlowState(),
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_BINDING,
                    message = buildMessage(user, context.locale),

                ),
                AnswerCallbackAction(callbackQuery.id),
            )
        )
    }

    private fun buildSendReportMessage(callbackQuery: CallbackQuery, state: SettingFlowState, locale: Locale): FlowResult<SettingFlowState> {
        val binding = "send_report_${UUID.randomUUID()}"
        state.promptBindings.add(binding)
        state.pendingAction = SettingPendingAction(true)
        return FlowResult(
            stepKey = StepKey.SEND_REPORT.key,
            payload = state,
            actions = listOf(
                SendMessageAction(
                    bindingKey =  binding,
                    message = FlowMessage(
                        flowKey = key,
                        stepKey = StepKey.SEND_REPORT.key,
                        model = SendReportModel(
                            i18nService.i18n("flow.settings.send_report", locale)
                        ),
                        inlineButtons = listOf(
                            FlowInlineButton(
                                text = i18nService.i18n("flow.button.cancel", locale),
                                payload = FlowCallbackPayload(key.value, "SEND_REPORT_CANCEL"),
                            )
                        )
                    ),
                ),
                AnswerCallbackAction(callbackQuery.id),
            )
        )
    }

    private fun buildSendReportCancelMessage(callbackQuery: CallbackQuery, state: SettingFlowState): FlowResult<SettingFlowState> {
        val cleanUp = FlowUtils.cleanupPromptActions(state.promptBindings)
        cleanUp += AnswerCallbackAction(callbackQuery.id)
        return FlowResult(
            stepKey = StepKey.MAIN.key,
            payload = SettingFlowState(),
            actions = cleanUp
        )
    }


    private fun processNotify(user: User, type: NotificationType) {
        val notify = user.notificationSubscribe.firstOrNull { it.type == type }
        if (notify != null) {
            notify.enabled = !notify.enabled
        } else {
            user.notificationSubscribe.add(
                NotificationSubscribe(
                    user = user,
                    type = type,
                    enabled = true,
                ),
            )
        }
    }

    private fun buildMessage(user: User?, locale: Locale): FlowMessage {
        val profile = user?.profile
        val model = SettingsViewModel(
            siegeEnabled = user?.notificationSubscribe?.firstOrNull { it.type == NotificationType.SIEGE }?.enabled == true,
            mineEnabled = user?.notificationSubscribe?.firstOrNull { it.type == NotificationType.MINE }?.enabled == true,
            cbEnabled = profile?.settings?.resourcesCb ?: false,
            quickResourceEnabled = profile?.settings?.resourcesQuickChange ?: false,
        )
        return FlowMessage(
            flowKey = key,
            stepKey = StepKey.MAIN.key,
            model = model,
            inlineButtons = buildButtons(model, locale)
        )
    }

    private fun buildButtons(
        model: SettingsViewModel,
        locale: Locale
    ): List<FlowInlineButton> {
        return listOf(
            buildButton(getText(model, SIEGE, locale), "NOTIFY_SIEGE", 1),
            buildButton(getText(model, MINE, locale), "NOTIFY_MINE", 2),
            buildButton(getText(model, CB_ENABLE, locale), "CB_ENABLE", 3),
            buildButton(getText(model, QUICK_RESOURCE, locale), "QUICK_RESOURCES", 4),
            buildButton(i18nService.i18n(
                code = "buttons.settings.send_report",
                locale = locale,
                default = "✍\uFE0F Отправить пожелание\\сообщение об ошибке")
                , "SEND_REPORT", 99),
        )
    }

    private fun getText(model: SettingsViewModel, type: SettingsType, locale: Locale): String {
        return when (type) {
            SIEGE -> {
                i18nService.i18n(
                    code = if (model.siegeEnabled) "buttons.settings.siege.enabled" else "buttons.settings.siege.disabled",
                    locale = locale,
                    default = if (model.siegeEnabled) "\uD83D\uDCF4 Включить в момент осады" else "\uD83D\uDCF4 Включить за 5 минут до осады"
                )
            }
            MINE -> {
                i18nService.i18n(
                    code = if (model.mineEnabled) "buttons.settings.mine.disable" else "buttons.settings.mine.enable",
                    locale = locale,
                    default = if (model.mineEnabled) "❌ Отключить КШ" else "✅ Включить КШ"
                )
            }
            CB_ENABLE -> {
                i18nService.i18n(
                    code = if (model.cbEnabled) "buttons.settings.cb.disable" else "buttons.settings.cb.enable",
                    locale = locale,
                    default = if (model.cbEnabled) "❌ Отключить учет КБ" else "✅ Включить учет КБ"
                )
            }
            QUICK_RESOURCE -> {
                i18nService.i18n(
                    code = if (model.quickResourceEnabled) "buttons.settings.quick.disable" else "buttons.settings.quick.enable",
                    locale = locale,
                    default = if (model.quickResourceEnabled) "❌ Отключить быстрый учет" else "✅ Включить быстрый учет"
                )
            }
        }
    }

    private fun buildButton(text: String, data: String, row: Int) =
        FlowInlineButton(
            text = text,
            payload = FlowCallbackPayload(
                flow = key.value,
                data = data
            ),
            row = row,
        )

    companion object {
        private const val MAIN_MESSAGE_BINDING = "settings_main"
    }
}

enum class SettingsType {
    SIEGE, MINE, CB_ENABLE, QUICK_RESOURCE
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
    val promptBindings: MutableList<String> = mutableListOf(),
)

data class SettingPendingAction(var sendReport: Boolean = true)
