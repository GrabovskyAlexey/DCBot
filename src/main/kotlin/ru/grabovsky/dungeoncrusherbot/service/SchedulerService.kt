package ru.grabovsky.dungeoncrusherbot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.repository.ResourceServerStateRepository
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.NotifyHistoryService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.TelegramBotService
import java.time.*
import kotlin.math.absoluteValue

@Service
class SchedulerService(
    private val userRepository: UserRepository,
    private val telegramBotService: TelegramBotService,
    private val notifyHistoryService: NotifyHistoryService,
    private val resourceServerStateRepository: ResourceServerStateRepository
) {

    @Scheduled(cron = "\${scheduler.cron.siege}")
    fun scheduleSiege() {
        val dateTime = LocalDateTime.now(ZoneId.ofOffset("UTC", ZoneOffset.of("+03:00:00")))
        if (dateTime.isNotSiegeTime()) {
            return
        }
        sendSiegeNotification(dateTime.toLocalTime(), false)
    }

    @Scheduled(cron = "\${scheduler.cron.before-siege}")
    fun scheduleBeforeSiege() {
        val dateTime = LocalDateTime.now(ZoneId.ofOffset("UTC", ZoneOffset.of("+03:00:00"))).plusMinutes(5)
        if (dateTime.isNotSiegeTime()) {
            return
        }
        sendSiegeNotification(dateTime.toLocalTime(), true)
    }

    private fun sendSiegeNotification(time: LocalTime, isBefore: Boolean) {
        logger.debug { "Schedule siege time: $time" }
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
        return resourceServerStateRepository.findAllByUserUserIdAndNotifyDisableTrue(user.userId)
            .map { it.server.id }
            .toSet()
    }

    private fun LocalTime.equalsWithGap(time: LocalTime, minutes: Int) =
        Duration.between(this, time).seconds.absoluteValue < minutes * 60

    private fun LocalDateTime.isNotSiegeTime() =
        (this.dayOfWeek == DayOfWeek.SUNDAY && this.hour > 21) ||
                (this.dayOfWeek == DayOfWeek.MONDAY && this.hour < 3)

    @Scheduled(cron = "\${scheduler.cron.delete-old-notify}")
    fun deleteOldNotify() {
        telegramBotService.deleteOldNotify()
        notifyHistoryService.deleteOldEvents()
    }

    @Scheduled(cron = "\${scheduler.cron.clear-disable-notify}")
    fun clearDisableNotify() {
        logger.info { "Start enable all server siege notification" }
        val states = resourceServerStateRepository.findAllByNotifyDisableTrue()
        if (states.isEmpty()) {
            return
        }
        states.onEach { state ->
            state.notifyDisable = false
        }
        resourceServerStateRepository.saveAllAndFlush(states)
    }

    @Scheduled(cron = "\${scheduler.cron.clan-mine}")
    fun sendClanMineNotification() {
        val users = userRepository.findAllNotBlockedUser()
        val usersToNotify = users
            .filter { user ->
                user.notificationSubscribe.firstOrNull { it.type == NotificationType.MINE }?.enabled == true
            }

        usersToNotify.forEach {
            val result = telegramBotService.sendNotification(it.userId, NotificationType.MINE)
            if (!result) {
                processBlockedUser(it)
            }
        }
    }

    private fun processBlockedUser(user: User) {
        logger.warn { "User: ${user.userName ?: user.firstName} block bot" }
        user.profile?.let {
            it.isBlocked = true
            userRepository.save(user)
        }
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}

