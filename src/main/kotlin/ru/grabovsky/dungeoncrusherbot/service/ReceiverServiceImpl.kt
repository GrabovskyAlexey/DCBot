package ru.grabovsky.dungeoncrusherbot.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.dungeoncrusherbot.service.interfaces.FlowStateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ReceiverService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowCallbackPayload
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowEngine
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKey
import ru.grabovsky.dungeoncrusherbot.util.LocaleUtils
import java.util.*

@Service
class ReceiverServiceImpl(
    private val userService: UserService,
    private val objectMapper: ObjectMapper,
    private val flowEngine: FlowEngine,
    private val flowStateService: FlowStateService,
) : ReceiverService {

    override fun execute(update: Update) {
        when {
            update.hasCallbackQuery() -> processCallback(update.callbackQuery)
            update.hasMessage() -> processMessage(update.message)
        }
    }

    private fun processMessage(message: Message) {
        val user = message.from
        userService.createOrUpdateUser(user)
        val flowState = flowStateService.findListFlow(user.id) ?: run {
            logger.debug { "Skip message ${message.messageId} from ${user.id}: no active flow" }
            return
        }
        val handled = flowEngine.onMessage(
            FlowKey(flowState.flowKey),
            user,
            resolveLocale(user),
            message
        )
        if (!handled) {
            logger.debug {
                "Active flow ${flowState.flowKey} ignored message ${message.messageId} from ${user.id}"
            }
        }
    }

    private fun processCallback(callbackQuery: CallbackQuery) {
        logger.debug {"Start process callback: $callbackQuery"}
        val user = callbackQuery.from
        userService.createOrUpdateUser(user)
        val payload = parseFlowPayload(callbackQuery.data)
        if (payload == null) {
            logger.warn { "Callback data has unexpected format: ${callbackQuery.data}" }
            return
        }
        val flowKey = FlowKey(payload.flow)
        val locale = resolveLocale(user)
        if (flowEngine.onCallback(flowKey, user, locale, callbackQuery, payload.data)) {
            return
        }
        if (!flowEngine.start(flowKey, user, locale)) {
            logger.warn { "Flow ${flowKey.value} not found for callback ${callbackQuery.data}" }
            return
        }
        val handled = flowEngine.onCallback(flowKey, user, locale, callbackQuery, payload.data)
        if (!handled) {
            logger.warn {
                "Callback ${callbackQuery.id ?: "unknown"} not handled even after restart for flow ${flowKey.value}"
            }
        }
    }

    private fun parseFlowPayload(data: String): FlowCallbackPayload? =
        runCatching {
            objectMapper.readValue(data, FlowCallbackPayload::class.java)
        }.getOrNull()

    private fun resolveLocale(user: User): Locale {
        val stored = userService.getUser(user.id)
        return LocaleUtils.resolve(stored?.language)
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}
