package ru.grabovsky.dungeoncrusherbot.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.dungeoncrusherbot.bot.commands.State
import ru.grabovsky.dungeoncrusherbot.dto.StartDto
import ru.grabovsky.dungeoncrusherbot.dto.ServerDto
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.repository.ServerRepository
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository
import kotlin.collections.toMutableList

@Service
class MessageService(
    private val messageServiceGenerate: MessageServiceGenerate,
    private val telegramClient: TelegramClient,
    private val userRepository: UserRepository,
    private val serverRepository: ServerRepository,
) {

    @Transactional
    fun sendMessage(user: User, chat: Chat, state: State, messageId: Int? = null) {
        when (state) {
            State.START -> sendStartMessage(user, chat)
            State.SUBSCRIBE -> sendSubscribeMessage(user, chat, messageId)
            else -> {}
        }
    }

    fun sendNotification(chatId: Long, servers: List<Server> = emptyList()){
        if(servers.isEmpty()) {
            return
        }
        val message = messageServiceGenerate.process(State.NOTIFICATION, ServerDto(servers.sortedBy { it.id }.map { it.id }))
        val sendMessage = SendMessage.builder()
            .chatId(chatId)
            .text(message)
            .build()

        sendMessage.enableMarkdown(true)

        telegramClient.execute(sendMessage)
    }

    private fun sendStartMessage(user: User, chat: Chat) {
        val message = messageServiceGenerate.process(State.START, StartDto(user.userName))
        val sendMessage = SendMessage.builder()
            .chatId(chat.id)
            .text(message)
            .build()

        sendMessage.enableMarkdown(true)

        telegramClient.execute(sendMessage)
    }


    private fun sendSubscribeMessage(user: User, chat: Chat, messageId: Int? = null) {
        val servers = userRepository.findUserByUserId(user.id)?.servers ?: mutableSetOf()
        val message = messageServiceGenerate.process(State.SUBSCRIBE, ServerDto(servers.sortedBy { it.id }.map { it.id }))

        val markup =
            generateSubscribeInlineMarkUp(servers).takeIf { it.keyboard.isNotEmpty() }

        val msg = if (messageId == null) {
            val msg = SendMessage.builder()
                .chatId(chat.id)
                .text(message)
                .replyMarkup(markup)
                .build()

            msg.enableMarkdown(true)
            telegramClient.execute(msg)
        } else {
            val msg = EditMessageText.builder()
                .chatId(chat.id)
                .text(message)
                .replyMarkup(markup)
                .messageId(messageId)
                .build()

            msg.enableMarkdown(true)
            telegramClient.execute(msg)
        }
    }


    fun generateSubscribeInlineMarkUp(servers: MutableCollection<Server>): InlineKeyboardMarkup {
        val inlineKeyboardButtonsInner: MutableList<InlineKeyboardButton> = mutableListOf()
        val inlineKeyboardButtons: MutableList<MutableList<InlineKeyboardButton>> = mutableListOf()


        val allServers = serverRepository.findAll()
        var count = 0
        for (server in allServers) {
            val isSubscribed = servers.any { it == server }
            val text = if (isSubscribed) {
                "✅ ${server.id}"
            } else {
                "❌ ${server.id}"
            }
            val callbackData = if (isSubscribed) {
                "UNSUBSCRIBE ${server.id}"
            } else {
                "SUBSCRIBE ${server.id}"
            }
            val button = InlineKeyboardButton.builder().text(text)
                .callbackData(callbackData)
                .build()
            inlineKeyboardButtonsInner.add(button)
            count++
            if (count >= 5) {
                count = 0
                inlineKeyboardButtons.add(inlineKeyboardButtonsInner.toMutableList())
                inlineKeyboardButtonsInner.clear()
            }
        }
        if (inlineKeyboardButtonsInner.isNotEmpty()) {
            inlineKeyboardButtons.add(inlineKeyboardButtonsInner.toMutableList())
            inlineKeyboardButtonsInner.clear()
        }
        val rows = inlineKeyboardButtons.map { InlineKeyboardRow(it) }
        return InlineKeyboardMarkup(rows)
    }
}