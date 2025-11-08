package ru.grabovsky.dungeoncrusherbot.strategy.flow.subscribe

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.*
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.buildMessage
import java.util.*
import ru.grabovsky.dungeoncrusherbot.entity.User as BotUser

@Component
class SubscribeFlow(
    private val userService: UserService,
    private val serverService: ServerService,
    private val i18nService: I18nService
) : FlowHandler<Unit> {
    override val key: FlowKey = FlowKeys.SUBSCRIBE
    override val payloadType: Class<Unit> = Unit::class.java

    override fun start(context: FlowStartContext): FlowResult<Unit> {
        val subscriptions = loadSubscriptions(context.user.id)
        return FlowResult(
            stepKey = SubscribeStepKey.MAIN.key,
            payload = Unit,
            actions = listOf(
                SendMessageAction(
                    bindingKey = MAIN_MESSAGE_BINDING,
                    message = buildMessage(context.locale, subscriptions)
                )
            )
        )
    }

    override fun onMessage(context: FlowContext<Unit>, message: Message): FlowResult<Unit>? =
        null

    override fun onCallback(
        context: FlowContext<Unit>,
        callbackQuery: CallbackQuery,
        data: String
    ): FlowResult<Unit>? {
        val (command, serverId) = parseCallbackData(data) ?: return FlowResult(
            stepKey = context.state.stepKey,
            payload = Unit,
            actions = listOf(AnswerCallbackAction(callbackQuery.id))
        )

        val user = loadUser(callbackQuery.from)
        val server = runCatching { serverService.getServerById(serverId) }.getOrNull()
            ?: return FlowResult(
                stepKey = context.state.stepKey,
                payload = Unit,
                actions = listOf(AnswerCallbackAction(callbackQuery.id))
            )

        when (command) {
            "SUBSCRIBE" -> user.servers.add(server)
            "UNSUBSCRIBE" -> user.servers.removeIf { it == server }
        }
        userService.saveUser(user)

        val subscriptions = user.servers.map(Server::id).sorted()
        return FlowResult(
            stepKey = SubscribeStepKey.MAIN.key,
            payload = Unit,
            actions = listOf(
                EditMessageAction(
                    bindingKey = MAIN_MESSAGE_BINDING,
                    message = buildMessage(context.locale, subscriptions)
                ),
                AnswerCallbackAction(callbackQuery.id)
            )
        )
    }

    private fun loadSubscriptions(userId: Long): List<Int> =
        userService.getUser(userId)?.servers?.map(Server::id)?.sorted().orEmpty()

    private fun loadUser(user: User): BotUser =
        userService.getUser(user.id)
            ?: BotUser(
                userId = user.id,
                firstName = user.firstName,
                lastName = user.lastName,
                userName = user.userName,
                language = user.languageCode
            )

    private fun buildMessage(locale: Locale, subscriptions: List<Int>): FlowMessage {
        val allServers = serverService.getAllServers().sortedBy { it.id }
        return key.buildMessage(
            step = SubscribeStepKey.MAIN,
            model = SubscribeViewModel(subscriptions),
            inlineButtons = buildButtons(locale, allServers, subscriptions)
        )
    }

    private fun buildButtons(
        locale: Locale,
        allServers: List<Server>,
        subscriptions: List<Int>
    ): List<FlowInlineButton> {
        val subscribedSet = subscriptions.toSet()
        var row = 0
        var col = 0
        return buildList {
            for (server in allServers) {
                val isSubscribed = server.id in subscribedSet
                add(
                    FlowInlineButton(
                        text = buttonLabel(locale, isSubscribed, server.id),
                        payload = FlowCallbackPayload(
                            flow = key.value,
                            data = "${if (isSubscribed) "UNSUBSCRIBE" else "SUBSCRIBE"}:${server.id}"
                        ),
                        row = row,
                        col = col,
                    )
                )
                col++
                if (col >= 5) {
                    col = 0
                    row++
                }
            }
        }
    }

    private fun buttonLabel(locale: Locale, isSubscribed: Boolean, serverId: Int): String {
        val code = if (isSubscribed) {
            "buttons.subscribe.subscribed"
        } else {
            "buttons.subscribe.available"
        }
        return i18nService.i18n(code, locale, serverId.toString(), serverId)
    }

    private fun parseCallbackData(data: String): Pair<String, Int>? {
        val parts = data.split(":")
        if (parts.size != 2) {
            return null
        }
        val command = parts[0]
        val id = parts[1].toIntOrNull() ?: return null
        return command to id
    }

    companion object {
        private const val MAIN_MESSAGE_BINDING = "subscribe_main"
    }
}

data class SubscribeViewModel(
    val servers: List<Int>
)

enum class SubscribeStepKey(override val key: String) : FlowStep {
    MAIN("main"),
}
