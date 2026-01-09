package ru.grabovsky.dungeoncrusherbot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import ru.grabovsky.dungeoncrusherbot.entity.*
import ru.grabovsky.dungeoncrusherbot.mapper.UserMapper
import ru.grabovsky.dungeoncrusherbot.repository.AdminMessageRepository
import ru.grabovsky.dungeoncrusherbot.repository.MazeRepository
import ru.grabovsky.dungeoncrusherbot.repository.ResourceServerStateRepository
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.FlowStateService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.AdminMessageDto
import ru.grabovsky.dungeoncrusherbot.strategy.dto.AdminReplyDto
import ru.grabovsky.dungeoncrusherbot.strategy.flow.admin.AdminMessageFlowState
import ru.grabovsky.dungeoncrusherbot.strategy.flow.admin.AdminMessageStep
import ru.grabovsky.dungeoncrusherbot.strategy.flow.admin.AdminMessageViewBuilder
import ru.grabovsky.dungeoncrusherbot.strategy.flow.admin.AdminPendingMessage
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.*
import ru.grabovsky.dungeoncrusherbot.util.LocaleUtils
import java.time.Instant
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val adminMessageRepository: AdminMessageRepository,
    private val mazeRepository: MazeRepository,
    private val resourceServerStateRepository: ResourceServerStateRepository,
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
        val updated = entity?.let { updateUser(it, user) }
            ?: createNewUser(UserMapper.fromTelegramToEntity(user))
        refreshExchangeBindings(updated)
        return updated
    }

    private fun updateUser(entity: User, user: TgUser): User {
        val profile = entity.profile ?: UserProfile(user = entity).also { entity.profile = it }
        val hasProfileChanges = entity.firstName != user.firstName ||
                entity.lastName != user.lastName ||
                entity.userName != user.userName ||
                entity.language != user.languageCode ||
                profile.isBlocked
        val usernameChanged = entity.userName != user.userName
        if (hasProfileChanges) {
            logger.info { "Update user: $user, entity: $entity" }
        }
        profile.isBlocked = false
        entity.firstName = user.firstName
        entity.lastName = user.lastName
        entity.userName = user.userName
        entity.language = user.languageCode
        entity.lastActionAt = Instant.now()
        val saved = userRepository.saveAndFlush(entity)
        if (usernameChanged && saved.userName != null) {
            updateExchangeUsernames(saved)
        }
        return saved
    }

    private fun createNewUser(userFromTelegram: User): User {
        logger.info { "Save new user: $userFromTelegram" }
        cleanOldMaze(userFromTelegram.userId)
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

    override fun updateBlockedStatus(userId: Long, isBlocked: Boolean) {
        val user = userRepository.findUserByUserId(userId)
        if (user == null) {
            logger.info { "Skip update blocked status for unknown user $userId" }
            return
        }
        val profile = user.profile ?: UserProfile(user = user).also { user.profile = it }
        if (profile.isBlocked == isBlocked) {
            return
        }
        profile.isBlocked = isBlocked
        userRepository.saveAndFlush(user)
        logger.info { "User $userId blocked status updated to $isBlocked" }
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

    override fun sendAdminMessage(user: TgUser, message: String, sourceMessageId: Int, sourceChatId: Long) {
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
                val locale = LocaleUtils.resolve(admin)
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

        if (sourceMessageId > 0) {
            runCatching {
                telegramFlowActionExecutor.execute(
                    user = user,
                    locale = LocaleUtils.defaultLocale,
                    currentBindings = emptyMap(),
                    actions = listOf(
                        SetReactionAction(
                            chatId = sourceChatId,
                            messageId = sourceMessageId,
                            emoji = "\uD83D\uDC4D"
                        )
                    )
                )
            }.onFailure {
                logger.warn { "Failed to set reaction for user ${user.userName ?: user.id}: ${it.message}" }
            }
        }
    }

    override fun sendAdminReply(admin: TgUser, targetUserId: Long, message: String, replyToMessageId: Int?) {
        val userEntity = getUser(targetUserId) ?: return
        val locale = LocaleUtils.resolve(userEntity)
        val targetUser = TgUser.builder()
            .id(userEntity.userId)
            .firstName(userEntity.firstName ?: "")
            .lastName(userEntity.lastName)
            .userName(userEntity.userName)
            .isBot(false)
            .build()

        val dto = AdminReplyDto(text = message)

        val replyMessage = FlowMessage(
            flowKey = FlowKeys.ADMIN_REPLY,
            stepKey = "main",
            model = dto,
            replyToMessageId = replyToMessageId
        )
        val initialAction = SendMessageAction(
            bindingKey = null,
            message = replyMessage
        )

        val result = runCatching {
            telegramFlowActionExecutor.execute(
                user = targetUser,
                locale = locale,
                currentBindings = emptyMap(),
                actions = listOf(initialAction)
            )
        }

        val failure = result.exceptionOrNull()
        if (failure != null) {
            if (replyToMessageId != null && failure is TelegramApiRequestException && failure.isReplyTargetMissing()) {
                logger.info { "Reply message not found for user ${targetUser.userName ?: targetUser.id}, send without reply binding" }
                telegramFlowActionExecutor.execute(
                    user = targetUser,
                    locale = locale,
                    currentBindings = emptyMap(),
                    actions = listOf(
                        SendMessageAction(
                            bindingKey = null,
                            message = replyMessage.copy(replyToMessageId = null)
                        )
                    )
                )
            } else {
                throw failure
            }
        }
    }

    override fun findByUsername(username: String): User? =
        userRepository.findByUserNameIgnoreCase(username)

    private fun cleanOldMaze(userId: Long) {
        val removed = runCatching { mazeRepository.deleteAllByUserId(userId) }.getOrDefault(0)
        if (removed > 0) {
            logger.info { "Removed $removed orphan maze records for user $userId" }
        }
    }

    private fun TelegramApiRequestException.isReplyTargetMissing(): Boolean {
        if (errorCode != 400) {
            return false
        }
        val details = (apiResponse ?: message ?: "").lowercase()
        return details.contains("message to be replied not found")
    }

    private fun refreshExchangeBindings(user: User) {
        val username = user.userName ?: return
        val states = resourceServerStateRepository.findAllByExchangeUsernameIgnoreCase(username)
        if (states.isEmpty()) return
        states.forEach { state ->
            if (state.exchangeUserId != user.userId) {
                state.exchangeUserId = user.userId
            }
        }
        resourceServerStateRepository.saveAll(states)
    }

    private fun updateExchangeUsernames(user: User) {
        val username = user.userName ?: return
        val states = resourceServerStateRepository.findAllByExchangeUserId(user.userId)
        if (states.isEmpty()) return
        states.forEach { state ->
            state.exchangeUsername = username
        }
        resourceServerStateRepository.saveAll(states)
    }
}

