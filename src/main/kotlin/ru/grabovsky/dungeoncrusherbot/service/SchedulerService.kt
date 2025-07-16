package ru.grabovsky.dungeoncrusherbot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.NotifyHistoryService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.TelegramBotService
import java.time.*
import kotlin.math.absoluteValue

@Service
class SchedulerService(
    private val userRepository: UserRepository,
    private val telegramBotService: TelegramBotService,
    private val notifyHistoryService: NotifyHistoryService
) {

    @Scheduled(cron = "0 0 * ? * *")
    fun scheduleSiege() {
        val dateTime = LocalDateTime.now(ZoneId.ofOffset("UTC", ZoneOffset.of("+03:00:00")))
        if (dateTime.isNotSiegeTime()) {
            return
        }
        sendSiegeNotification(dateTime.toLocalTime(), false)
    }

    @Scheduled(cron = "0 55 * ? * * ")
    fun scheduleBeforeSiege() {
        val dateTime = LocalDateTime.now(ZoneId.ofOffset("UTC", ZoneOffset.of("+03:00:00"))).plusMinutes(5)
        if (dateTime.isNotSiegeTime()) {
            return
        }
        sendSiegeNotification(dateTime.toLocalTime(), true)
    }

    private fun sendSiegeNotification(time: LocalTime, isBefore: Boolean) {
        logger.info { "Schedule siege time: $time" }
        val users = userRepository.findAllNotBlockedUser()
        val usersToNotify = users
            .filter { user ->
                if (isBefore) {
                    user.notificationSubscribe.any { it.type == NotificationType.SIEGE && it.enabled }
                } else {
                    user.notificationSubscribe.any { it.type == NotificationType.SIEGE && !it.enabled }
                            || user.notificationSubscribe.none { it.type == NotificationType.SIEGE }
                }
            }
            .associate {
                it.userId to it.servers.filter { server ->
                    server.sieges.any { siege ->
                        siege.siegeTime.equalsWithGap(time, 1)
                    }
                }.filter { server ->
                    !getDisabledNotificationServer(it).contains(server.id)
                }
            }.filterValues { it.isNotEmpty() }
        usersToNotify.forEach {
            val result = telegramBotService.sendNotification(it.key, NotificationType.SIEGE, it.value, isBefore)
            if (!result) {
                val user = users.firstOrNull { u -> u.userId == it.key }
                user?.let { u -> processBlockedUser(u) }
            }
        }
    }

    private fun getDisabledNotificationServer(user: User): Set<Int> {
        return user.resources?.data?.servers?.filter { it.value.notifyDisable }?.keys ?: emptySet()
    }

    private fun LocalTime.equalsWithGap(time: LocalTime, minutes: Int) =
        Duration.between(this, time).seconds.absoluteValue < minutes * 60

    private fun LocalDateTime.isNotSiegeTime() =
        (this.dayOfWeek == DayOfWeek.SUNDAY && this.hour > 21) ||
                (this.dayOfWeek == DayOfWeek.MONDAY && this.hour < 3)

    @Scheduled(cron = "0 0 * ? * *")
    fun deleteOldNotify() {
        telegramBotService.deleteOldNotify()
        notifyHistoryService.deleteOldEvents()
    }

    @Scheduled(cron = "0 0 0 ? * MON")
    fun clearDisableNotify() {
        logger.info { "Start enable all server siege notification" }
        val users = userRepository.findAll()
        users.onEach { user ->
            user.resources?.data?.servers?.onEach { server ->
                server.value.notifyDisable = false
            }
        }
        userRepository.saveAllAndFlush(users)
    }

    @Scheduled(cron = "0 59 23,11 ? * *")
    fun sendClanMineNotification() {
        val users = userRepository.findAllNotBlockedUser()
        val usersToNotify = users
            .filter { user -> user.notificationSubscribe.any { it.type == NotificationType.MINE } }

        usersToNotify.forEach {
            val result = telegramBotService.sendNotification(it.userId, NotificationType.MINE)
            if (!result) {
                processBlockedUser(it)
            }
        }
    }

    private fun processBlockedUser(user: User) {
        logger.warn { "User: ${user.userName ?: user.firstName} block bot" }
        user.isBlocked = true
        userRepository.save(user)
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}

