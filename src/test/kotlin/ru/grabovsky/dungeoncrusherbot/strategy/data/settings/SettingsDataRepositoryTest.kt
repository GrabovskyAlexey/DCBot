package ru.grabovsky.dungeoncrusherbot.strategy.data.settings

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.entity.NotificationSubscribe
import ru.grabovsky.dungeoncrusherbot.entity.NotificationType
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.entity.UserSettings
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class SettingsDataRepositoryTest : ShouldSpec({
    val userService = mockk<UserService>()
    val repository = SettingsDataRepository(userService)

    should("отражать включенные подписки и настройки ресурсов") {
        val entityUser = User(
            userId = 600L,
            firstName = "Settings",
            lastName = null,
            userName = "settings"
        ).apply {
            settings = UserSettings(resourcesCb = true, resourcesQuickChange = false)
            notificationSubscribe.add(NotificationSubscribe(user = this, type = NotificationType.SIEGE, enabled = true))
            notificationSubscribe.add(NotificationSubscribe(user = this, type = NotificationType.MINE, enabled = false))
        }
        every { userService.getUser(600L) } returns entityUser
        val tgUser = mockk<TgUser>(relaxed = true) {
            every { id } returns 600L
        }

        val dto = repository.getData(tgUser)

        dto.siegeEnabled shouldBe true
        dto.mineEnabled shouldBe false
        dto.cbEnabled shouldBe true
        dto.quickResourceEnabled shouldBe false
    }
})
