package ru.grabovsky.dungeoncrusherbot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import ru.grabovsky.dungeoncrusherbot.entity.Maze
import ru.grabovsky.dungeoncrusherbot.entity.NotificationSubscribe
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.mapper.UserMapper
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Service
class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {
    override fun createOrUpdateUser(user: TgUser): User {
        val entity = userRepository.findUserByUserId(user.id)
        val userFromTelegram = UserMapper.fromTelegramToEntity(user)
        when {
            userFromTelegram.userId != entity?.userId -> createNewUser(userFromTelegram)
            userFromTelegram != entity -> updateUser(entity, user)
        }
        return userFromTelegram
    }

    private fun updateUser(entity: User, user: TgUser) {
        logger.info { "Update user: $user" }
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


    companion object {
        val logger = KotlinLogging.logger {}
    }
}