package ru.grabovsky.dungeoncrusherbot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import ru.grabovsky.dungeoncrusherbot.entity.*
import ru.grabovsky.dungeoncrusherbot.mapper.UserMapper
import ru.grabovsky.dungeoncrusherbot.repository.AdminMessageRepository
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.AdminMessageDto
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowActionExecutor
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowMessage
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.SendMessageAction
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.ADD_NOTE
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode.REMOVE_NOTE
import ru.grabovsky.dungeoncrusherbot.util.LocaleUtils
import java.time.Instant
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val adminMessageRepository: AdminMessageRepository,
    private val telegramFlowActionExecutor: FlowActionExecutor
) : UserService {
    override fun createOrUpdateUser(user: TgUser): User {
        val entity = userRepository.findUserByUserId(user.id)
        return entity?.let { updateUser(it, user) }
            ?: createNewUser(UserMapper.fromTelegramToEntity(user))
    }

    private fun updateUser(entity: User, user: TgUser): User {
        val hasProfileChanges = entity.firstName != user.firstName ||
                entity.lastName != user.lastName ||
                entity.userName != user.userName ||
                entity.language != user.languageCode ||
                entity.isBlocked
        if (hasProfileChanges) {
            logger.info { "Update user: $user, entity: $entity" }
        }
        entity.isBlocked = false
        entity.firstName = user.firstName
        entity.lastName = user.lastName
        entity.userName = user.userName
        entity.language = user.languageCode
        entity.lastActionAt = Instant.now()
        return userRepository.saveAndFlush(entity)
    }

    private fun createNewUser(userFromTelegram: User): User {
        logger.info { "Save new user: $userFromTelegram" }
        userFromTelegram.apply {
            this.lastActionAt = Instant.now()
            this.maze = Maze(user = this)
            this.notificationSubscribe.addAll(
                listOf(
                    NotificationSubscribe(user = this, type = NotificationType.SIEGE, enabled = true),
                    NotificationSubscribe(user = this, type = NotificationType.MINE, enabled = false)
                )
            )
        }
        val saved = userRepository.saveAndFlush(userFromTelegram)
        logger.info { "Save user entity with id = ${saved.userId}" }
        return saved
    }

    override fun saveUser(user: User) {
        userRepository.saveAndFlush(user)
    }

    @Transactional
    override fun getUser(userId: Long) = userRepository.findUserByUserId(userId)

    override fun processNote(user: User, note: String, state: StateCode) {
        when (state) {
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
            val admin = TgUser.builder()
                .id(it.userId)
                .firstName(it.firstName!!)
                .lastName(it.lastName)
                .isBot(false)
                .build()
            telegramFlowActionExecutor.execute(
                user = admin, locale = LocaleUtils.resolve(it.language), actions = listOf(
                    SendMessageAction(
                        bindingKey = "admin_message",
                        message = FlowMessage(
                            flowKey = FlowKeys.ADMIN_MESSAGE,
                            stepKey = "main",
                            model = dto,
                        )
                    )
                ),
                currentBindings = emptyMap()
            )
//            eventPublisher.publishEvent(TelegramAdminMessageEvent(user, SEND_REPORT, it.userId, dto))
        }
    }

    private fun addNote(user: User, note: String) {
        if (user.notes.size >= 20) {
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
