package ru.grabovsky.dungeoncrusherbot.strategy.data.subscribe

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class SubscribeDataRepositoryTest : ShouldSpec({
    val userService = mockk<UserService>()
    val repository = SubscribeDataRepository(userService)

    should("provide sorted server identifiers for a user") {
        val entityUser = User(710L, "Tester", null, "tester").apply {
            servers.addAll(setOf(Server(5, "A"), Server(1, "B"), Server(3, "C")))
        }
        every { userService.getUser(710L) } returns entityUser
        val tgUser = mockk<TgUser>(relaxed = true) { every { id } returns 710L }

        val dto = repository.getData(tgUser)

        dto.servers shouldContainExactly listOf(1, 3, 5)
    }
})
