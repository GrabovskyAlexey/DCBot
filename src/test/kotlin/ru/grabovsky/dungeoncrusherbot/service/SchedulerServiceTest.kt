package ru.grabovsky.dungeoncrusherbot.service

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import ru.grabovsky.dungeoncrusherbot.entity.*
import ru.grabovsky.dungeoncrusherbot.repository.ResourceServerStateRepository
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.NotifyHistoryService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.TelegramBotService
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class SchedulerServiceTest : ShouldSpec({

    val userRepository = mockk<UserRepository>()
    val telegramBotService = mockk<TelegramBotService>()
    val notifyHistoryService = mockk<NotifyHistoryService>()
    val resourceServerStateRepository = mockk<ResourceServerStateRepository>()
    val scheduler = SchedulerService(userRepository, telegramBotService, notifyHistoryService, resourceServerStateRepository)

    beforeTest {
        clearMocks(userRepository, telegramBotService, notifyHistoryService, resourceServerStateRepository, answers = true)
    }

    should("send siege notifications and block users when delivery fails") {
        mockkStatic(LocalDateTime::class)
        val baseTime = LocalDateTime.of(2025, 9, 23, 10, 0)
        every { LocalDateTime.now(any<ZoneId>()) } returns baseTime

        val successfulUser = createSiegeUser(1L, enabled = false)
        val failedUser = createSiegeUser(2L, enabled = false)
        every { userRepository.findAllNotBlockedUser() } returns listOf(successfulUser, failedUser)
        every { resourceServerStateRepository.findAllByUserUserIdAndNotifyDisableTrue(1L) } returns emptyList()
        every { resourceServerStateRepository.findAllByUserUserIdAndNotifyDisableTrue(2L) } returns emptyList()
        every { telegramBotService.sendNotification(1L, NotificationType.SIEGE, any(), false) } returns true
        every { telegramBotService.sendNotification(2L, NotificationType.SIEGE, any(), false) } returns false
        every { userRepository.save(failedUser) } returns failedUser

        scheduler.scheduleSiege()

        verify { telegramBotService.sendNotification(1L, NotificationType.SIEGE, any(), false) }
        verify { telegramBotService.sendNotification(2L, NotificationType.SIEGE, any(), false) }
        failedUser.profile!!.isBlocked shouldBe true
        verify { userRepository.save(failedUser) }

        unmockkStatic(LocalDateTime::class)
    }

    should("send before siege notifications when enabled") {
        mockkStatic(LocalDateTime::class)
        val baseTime = LocalDateTime.of(2025, 9, 23, 9, 55)
        every { LocalDateTime.now(any<ZoneId>()) } returns baseTime

        val user = createSiegeUser(3L, enabled = true)
        every { userRepository.findAllNotBlockedUser() } returns listOf(user)
        every { resourceServerStateRepository.findAllByUserUserIdAndNotifyDisableTrue(3L) } returns emptyList()
        every { telegramBotService.sendNotification(3L, NotificationType.SIEGE, any(), true) } returns true

        scheduler.scheduleBeforeSiege()

        verify { telegramBotService.sendNotification(3L, NotificationType.SIEGE, any(), true) }

        unmockkStatic(LocalDateTime::class)
    }

    should("delegate deletion of notifications to services") {
        justRun { telegramBotService.deleteOldNotify() }
        justRun { notifyHistoryService.deleteOldEvents() }

        scheduler.deleteOldNotify()

        verify { telegramBotService.deleteOldNotify() }
        verify { notifyHistoryService.deleteOldEvents() }
    }

    should("clear disabled siege notifications and persist users") {
        val user = createSiegeUser(5L, enabled = false)
        val state = ResourceServerState(
            id = 77L,
            user = user,
            server = Server(5, "server5"),
            notifyDisable = true
        )
        every { resourceServerStateRepository.findAllByNotifyDisableTrue() } returns listOf(state)
        every { resourceServerStateRepository.saveAllAndFlush(listOf(state)) } returns listOf(state)

        scheduler.clearDisableNotify()

        state.notifyDisable shouldBe false
        verify { resourceServerStateRepository.saveAllAndFlush(listOf(state)) }
    }

    should("send clan mine notifications and block failures") {
        val mineUserSuccess = createMineUser(10L, enabled = true)
        val mineUserFail = createMineUser(11L, enabled = true)
        every { userRepository.findAllNotBlockedUser() } returns listOf(mineUserSuccess, mineUserFail)
        every { telegramBotService.sendNotification(10L, NotificationType.MINE, emptyList(), null) } returns true
        every { telegramBotService.sendNotification(11L, NotificationType.MINE, emptyList(), null) } returns false
        every { userRepository.save(mineUserFail) } returns mineUserFail

        scheduler.sendClanMineNotification()

        verify { telegramBotService.sendNotification(10L, NotificationType.MINE, emptyList(), null) }
        verify { telegramBotService.sendNotification(11L, NotificationType.MINE, emptyList(), null) }
        mineUserFail.profile!!.isBlocked shouldBe true
        verify { userRepository.save(mineUserFail) }
    }
})

private fun createSiegeUser(id: Long, enabled: Boolean): User {
    val user = User(id, "User$id", null, "user$id").apply {
        profile = UserProfile(userId = id, user = this)
    }
    val server = Server(id.toInt(), "server$id", mutableSetOf(Siege(id.toInt(), LocalTime.of(10, 0))))
    user.servers.add(server)
    val subscribe = NotificationSubscribe(user = user, type = NotificationType.SIEGE, enabled = enabled)
    user.notificationSubscribe.add(subscribe)
    return user
}

private fun createMineUser(id: Long, enabled: Boolean): User {
    val user = User(id, "User$id", null, "user$id").apply {
        profile = UserProfile(userId = id, user = this)
    }
    val subscribe = NotificationSubscribe(user = user, type = NotificationType.MINE, enabled = enabled)
    user.notificationSubscribe.add(subscribe)
    return user
}

