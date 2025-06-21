package ru.grabovsky.dungeoncrusherbot.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import ru.grabovsky.dungeoncrusherbot.bot.commands.State
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.repository.ServerRepository
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository

@Service
class ReceiverService(
    private val userRepository: UserRepository,
    private val serverRepository: ServerRepository,
    private val messageService: MessageService
) {

    fun handleUpdate(update: Update) {
        if (update.hasCallbackQuery()) {
            processCallback(update.callbackQuery)
        }
    }

    private fun processCallback(callback: CallbackQuery) {
        val data = callback.data.split(" ")
        val server = serverRepository.findServerById(data[1].toInt()) ?: throw EntityNotFoundException("Not found server with id: ${data[1]}")
        val tgUser = callback.from
        val user = userRepository.findUserByUserId(tgUser.id)
            ?: User(tgUser.id, tgUser.firstName, tgUser.lastName, tgUser.userName)
        when(data[0]) {
            "SUBSCRIBE" -> user.servers.add(server)
            "UNSUBSCRIBE" -> user.servers.removeIf { it == server }
        }
        userRepository.save(user)
        messageService.sendMessage(tgUser, callback.message.chat, State.SUBSCRIBE, callback.message.messageId)
    }
}