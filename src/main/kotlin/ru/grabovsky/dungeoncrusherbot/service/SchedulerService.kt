package ru.grabovsky.dungeoncrusherbot.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.TelegramBotService
import java.time.*

@Service
class SchedulerService(
    private val userRepository: UserRepository,
    private val telegramBotService: TelegramBotService
) {

    @Scheduled(cron = "0 0 * ? * *")
    fun schedule() {
        val dateTime = LocalDateTime.now(ZoneId.ofOffset("UTC", ZoneOffset.of("+03:00:00")))
        if (dateTime.isNotSiegeTime()) {
            return
        }
        val time = dateTime.toLocalTime()
        val users = userRepository.findAll()
        val usersToNotify = users.associate {
            it.userId to it.servers.filter { server ->
                server.sieges.any { siege ->
                    siege.siegeTime.equalsWithGap(time, 2)
                }
            }
        }.filterValues { it.isNotEmpty() }
        usersToNotify.forEach {
            telegramBotService.sendNotification(it.key, it.value)
        }
    }

    private fun LocalTime.equalsWithGap(time: LocalTime, minutes: Long) =
        this.isAfter(time.minusMinutes(minutes)) && this.isBefore(time.plusMinutes(minutes))

    private fun LocalDateTime.isNotSiegeTime() =
        (this.dayOfWeek == DayOfWeek.SUNDAY && this.hour > 21) ||
                (this.dayOfWeek == DayOfWeek.MONDAY && this.hour < 3)

}

