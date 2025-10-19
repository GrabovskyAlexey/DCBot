package ru.grabovsky.dungeoncrusherbot.strategy.flow.subscribe

import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.entity.User as BotUser
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.AnswerCallbackAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.EditMessageAction
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowCallbackContext
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowCallbackPayload
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
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import java.util.Locale

private const val MAIN_MESSAGE_BINDING = "subscribe_main"

@Component
class SubscribeFlow(
    private val userService: UserService,
    private val serverService: ServerService,
    private val messageSource: MessageSource,
) : FlowHandler<Unit> {
    override val key: FlowKey = FlowKeys.SUBSCRIBE
    override val payloadType: Class<Unit> = Unit::class.java

    override fun start(context: FlowStartContext): FlowResult<Unit> {
        val subscriptions = loadSubscriptions(context.user.id)
        return FlowResult(
            stepKey = SubscribeStep.MAIN.key,
            payload = Unit,
            actions = listOf(
                SendMessageAction(
                    bindingKey = MAIN_MESSAGE_BINDING,
                    message = buildMessage(context.locale, subscriptions)
                )
            )
        )
    }

    override fun onMessage(context: FlowMessageContext<Unit>, message: Message): FlowResult<Unit>? =
        null

    override fun onCallback(
        context: FlowCallbackContext<Unit>,
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
            SubscribeCommand.SUBSCRIBE -> user.servers.add(server)
            SubscribeCommand.UNSUBSCRIBE -> user.servers.removeIf { it == server }
        }
        userService.saveUser(user)

        val subscriptions = user.servers.map(Server::id).sorted()
        return FlowResult(
            stepKey = SubscribeStep.MAIN.key,
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
        return FlowMessage(
            flowKey = key,
            stepKey = SubscribeStep.MAIN.key,
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
                            data = "${if (isSubscribed) SubscribeCommand.UNSUBSCRIBE.value else SubscribeCommand.SUBSCRIBE.value} ${server.id}"
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
        return messageSource.getMessage(code, arrayOf<Any>(serverId), serverId.toString(), locale)
            ?: serverId.toString()
    }

    private fun parseCallbackData(data: String): Pair<SubscribeCommand, Int>? {
        val parts = data.split(" ")
        if (parts.size != 2) {
            return null
        }
        val command = SubscribeCommand.fromRaw(parts[0]) ?: return null
        val id = parts[1].toIntOrNull() ?: return null
        return command to id
    }
}

data class SubscribeViewModel(
    val servers: List<Int>
)

enum class SubscribeStep(override val key: String) : FlowStep {
    MAIN("main")
}

private enum class SubscribeCommand(val value: String) {
    SUBSCRIBE("SUBSCRIBE"),
    UNSUBSCRIBE("UNSUBSCRIBE");

    companion object {
        fun fromRaw(raw: String): SubscribeCommand? =
            entries.firstOrNull { it.value.equals(raw, ignoreCase = true) }
    }
}
