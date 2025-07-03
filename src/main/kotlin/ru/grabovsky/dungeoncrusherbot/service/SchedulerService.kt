package ru.grabovsky.dungeoncrusherbot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
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
        val users = userRepository.findAll()
        val usersToNotify = users
            .filter { user -> user.notificationSubscribe.any{ it.type == NotificationType.SIEGE && it.enabled == isBefore} || !isBefore }
            .associate {
                it.userId to it.servers.filter { server ->
                    server.sieges.any { siege ->
                        siege.siegeTime.equalsWithGap(time, 1)
                    }
                }
            }.filterValues { it.isNotEmpty() }
        usersToNotify.forEach {
            telegramBotService.sendNotification(it.key, NotificationType.SIEGE, it.value)
        }
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

    @Scheduled(cron = "0 0 0,12 ? * *")
    fun sendClanMineNotification(){
        val users = userRepository.findAll()
        val usersToNotify = users
            .filter { user -> user.notificationSubscribe.any{ it.type == NotificationType.MINE } }

        usersToNotify.forEach {
            telegramBotService.sendNotification(it.userId, NotificationType.MINE)
        }
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}

