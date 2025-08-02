package ru.grabovsky.dungeoncrusherbot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import ru.grabovsky.dungeoncrusherbot.entity.AdminMessage
import ru.grabovsky.dungeoncrusherbot.entity.Maze
import ru.grabovsky.dungeoncrusherbot.entity.NotificationSubscribe
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.event.TelegramAdminMessageEvent
import ru.grabovsky.dungeoncrusherbot.mapper.UserMapper
import ru.grabovsky.dungeoncrusherbot.repository.AdminMessageRepository
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.AdminMessageDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.*
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val adminMessageRepository: AdminMessageRepository,
    private val eventPublisher: ApplicationEventPublisher
) : UserService {
    override fun createOrUpdateUser(user: TgUser): User {
        val entity = userRepository.findUserByUserId(user.id)
        val userFromTelegram = UserMapper.fromTelegramToEntity(user)
        when {
            userFromTelegram.userId != entity?.userId -> createNewUser(userFromTelegram)
            userFromTelegram != entity || entity.isBlocked -> updateUser(entity, user)
        }
        return userFromTelegram
    }

    private fun updateUser(entity: User, user: TgUser) {
        logger.info { "Update user: $user, entity: $entity" }
        entity.isBlocked = false
        entity.firstName = user.firstName
        entity.lastName = user.lastName
        entity.userName = user.userName
        userRepository.saveAndFlush(entity)
    }

    private fun createNewUser(userFromTelegram: User) {
        logger.info { "Save new user: $userFromTelegram" }
        userFromTelegram.apply {
            this.maze = Maze(user = this)
            this.notificationSubscribe.addAll(
                listOf(
                    NotificationSubscribe(user = this, type = NotificationType.SIEGE, enabled = true),
                    NotificationSubscribe(user = this, type = NotificationType.MINE, enabled = false)
                )
            )
        }
        userRepository.saveAndFlush(userFromTelegram)
        logger.info { "Save user entity with id = ${userFromTelegram.userId}" }
    }

    override fun saveUser(user: User) {
        userRepository.saveAndFlush(user)
    }

    @Transactional
    override fun getUser(userId: Long) = userRepository.findUserByUserId(userId)

    override fun processNote(user: User, note: String, state: StateCode) {
        when(state) {
            ADD_NOTE -> addNote(user, note)
            REMOVE_NOTE -> removeNote(user, note)
            else -> {}
        }
    }

    override fun clearNotes(user: TgUser) {
        val userFromDb = userRepository.findUserByUserId(user.id) ?: return
        userFromDb.notes.clear()
        userRepository.saveAndFlush(userFromDb)
    }

    override fun sendAdminMessage(user: TgUser, message: String) {
        val dto = AdminMessageDto(user.firstName, user.userName, user.id, message)
        val entity = AdminMessage(userId = user.id, message = message)
        adminMessageRepository.saveAndFlush(entity)
        userRepository.findAllNotBlockedUser().filter { it.isAdmin }.forEach {
            eventPublisher.publishEvent(TelegramAdminMessageEvent(user, SEND_REPORT, it.userId, dto))
        }
    }

    private fun addNote(user: User, note: String) {
        if(user.notes.size >= 20) {
            user.notes.removeFirst()
        }
        user.notes.add(note)
        userRepository.saveAndFlush(user)
    }

    private fun removeNote(user: User, note: String) {
        val id = note.toInt() - 1
        user.notes.removeAt(id)
        userRepository.saveAndFlush(user)
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}