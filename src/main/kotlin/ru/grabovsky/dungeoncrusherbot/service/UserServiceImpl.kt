package ru.grabovsky.dungeoncrusherbot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import ru.grabovsky.dungeoncrusherbot.entity.*
import ru.grabovsky.dungeoncrusherbot.mapper.UserMapper
import ru.grabovsky.dungeoncrusherbot.repository.AdminMessageRepository
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.FlowStateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.AdminMessageDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.AdminReplyDto
import ru.grabovsky.dungeoncrusherbot.strategy.flow.admin.AdminMessageFlowState
import ru.grabovsky.dungeoncrusherbot.strategy.flow.admin.AdminMessageStep
import ru.grabovsky.dungeoncrusherbot.strategy.flow.admin.AdminPendingMessage
import ru.grabovsky.dungeoncrusherbot.strategy.flow.admin.AdminMessageViewBuilder
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowActionExecutor
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStateSnapshot
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowMessage
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.SendMessageAction
import ru.grabovsky.dungeoncrusherbot.util.LocaleUtils
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowPayloadSerializer
import java.time.Instant
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val adminMessageRepository: AdminMessageRepository,
    private val flowStateService: FlowStateService,
    private val payloadSerializer: FlowPayloadSerializer,
    private val adminMessageViewBuilder: AdminMessageViewBuilder,
    private val telegramFlowActionExecutor: FlowActionExecutor
) : UserService {
    companion object {
        private const val NOTES_LIMIT = 20
        private val logger = KotlinLogging.logger {}
    }

    override fun createOrUpdateUser(user: TgUser): User {
        val entity = userRepository.findUserByUserId(user.id)
        return entity?.let { updateUser(it, user) }
            ?: createNewUser(UserMapper.fromTelegramToEntity(user))
    }

    private fun updateUser(entity: User, user: TgUser): User {
        val profile = entity.profile ?: UserProfile(user = entity).also { entity.profile = it }
        val hasProfileChanges = entity.firstName != user.firstName ||
                entity.lastName != user.lastName ||
                entity.userName != user.userName ||
                entity.language != user.languageCode ||
                profile.isBlocked
        if (hasProfileChanges) {
            logger.info { "Update user: $user, entity: $entity" }
        }
        profile.isBlocked = false
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
            lastActionAt = Instant.now()
            maze = Maze(user = this)
            notificationSubscribe.addAll(
                listOf(
                    NotificationSubscribe(user = this, type = NotificationType.SIEGE, enabled = true),
                    NotificationSubscribe(user = this, type = NotificationType.MINE, enabled = false)
                )
            )
            profile = UserProfile(user = this)
        }
        val saved = userRepository.saveAndFlush(userFromTelegram)
        logger.info { "Save user entity with id = ${saved.userId}" }
        return saved
    }

    override fun saveUser(user: User) {
        user.profile?.user = user
        userRepository.saveAndFlush(user)
    }

    override fun getUser(userId: Long): User? {
        val user = userRepository.findUserByUserId(userId) ?: return null
        user.profile?.user = user
        return user
    }

    override fun clearNotes(user: TgUser) {
        val userFromDb = getUser(user.id) ?: return
        val profile = userFromDb.profile ?: return
        profile.notes.clear()
        saveUser(userFromDb)
    }

    override fun addNote(userId: Long, note: String): Boolean {
        val trimmed = note.trim()
        if (trimmed.isEmpty()) {
            return false
        }
        val user = getUser(userId) ?: return false
        val profile = user.profile ?: return false
        val notes = profile.notes
        if (notes.size >= NOTES_LIMIT) {
            notes.removeFirst()
        }
        notes.add(trimmed)
        saveUser(user)
        return true
    }

    override fun removeNote(userId: Long, index: Int): Boolean {
        val user = getUser(userId) ?: return false
        val profile = user.profile ?: return false
        val position = index - 1
        if (position !in profile.notes.indices) {
            return false
        }
        profile.notes.removeAt(position)
        saveUser(user)
        return true
    }

    override fun sendAdminMessage(user: TgUser, message: String, sourceMessageId: Int) {
        val dto = AdminMessageDto(user.firstName, user.userName, user.id, message)
        val entity = adminMessageRepository.saveAndFlush(
            AdminMessage(userId = user.id, message = message, sourceMessageId = sourceMessageId)
        )

        userRepository.findAllNotBlockedUser()
            .onEach { existing ->
                existing.profile?.user = existing
            }
            .filter { it.profile?.isAdmin == true }
            .forEach { admin ->
                val locale = LocaleUtils.resolve(admin.language)
                val bindingKey = "admin_message_${entity.id}"

                val snapshot = flowStateService.load(admin.userId, FlowKeys.ADMIN_MESSAGE)
                val state = snapshot?.payload?.let {
                    payloadSerializer.deserialize(it, AdminMessageFlowState::class.java)
                } ?: AdminMessageFlowState()

                state.messages.removeIf { it.id == entity.id }
                state.messages += AdminPendingMessage(
                    id = entity.id!!,
                    dto = dto,
                    bindingKey = bindingKey,
                    sourceMessageId = entity.sourceMessageId,
                )

                val adminUser = TgUser.builder()
                    .id(admin.userId)
                    .firstName(admin.firstName ?: "")
                    .lastName(admin.lastName)
                    .userName(admin.userName)
                    .isBot(false)
                    .build()

                val currentBindings = snapshot?.messageBindings ?: emptyMap()
                val mutation = telegramFlowActionExecutor.execute(
                    user = adminUser,
                    locale = locale,
                    currentBindings = currentBindings,
                    actions = listOf(
                        SendMessageAction(
                            bindingKey = bindingKey,
                            message = adminMessageViewBuilder.buildInboxMessage(dto, entity.id!!, locale)
                        )
                    )
                )

                val updatedBindings = currentBindings.toMutableMap().apply {
                    putAll(mutation.replacements)
                    mutation.removed.forEach { remove(it) }
                }

                flowStateService.save(
                    FlowStateSnapshot(
                        userId = admin.userId,
                        flowKey = FlowKeys.ADMIN_MESSAGE,
                        stepKey = AdminMessageStep.MAIN.key,
                        payload = payloadSerializer.serialize(state),
                        messageBindings = updatedBindings
                    )
                )
            }
    }

    override fun sendAdminReply(admin: TgUser, targetUserId: Long, message: String, replyToMessageId: Int?) {
        val userEntity = getUser(targetUserId) ?: return
        val locale = LocaleUtils.resolve(userEntity.language)
        val targetUser = TgUser.builder()
            .id(userEntity.userId)
            .firstName(userEntity.firstName ?: "")
            .lastName(userEntity.lastName)
            .userName(userEntity.userName)
            .isBot(false)
            .build()

        val dto = AdminReplyDto(text = message)

        telegramFlowActionExecutor.execute(
            user = targetUser,
            locale = locale,
            currentBindings = emptyMap(),
            actions = listOf(
                SendMessageAction(
                    bindingKey = null,
                    message = FlowMessage(
                        flowKey = FlowKeys.ADMIN_REPLY,
                        stepKey = "main",
                        model = dto,
                        replyToMessageId = replyToMessageId
                    )
                )
            )
        )
    }
}
