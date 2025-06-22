package ru.grabovsky.dungeoncrusherbot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
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
        val userFromTelegram= UserMapper.fromTelegramToEntity(user)
        if (userFromTelegram != entity) {
            userRepository.saveAndFlush(userFromTelegram)
            logger.info { "Save user entity with id = ${user.id}" }
        }
        return userFromTelegram
    }

    override fun saveUser(user: User) {
        userRepository.saveAndFlush(user)
    }

    override fun getUser(userId: Long) = userRepository.findUserByUserId(userId)

    companion object {
        val logger = KotlinLogging.logger {}
    }
}