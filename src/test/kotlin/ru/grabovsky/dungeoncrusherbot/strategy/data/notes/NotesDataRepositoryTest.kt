package ru.grabovsky.dungeoncrusherbot.strategy.data.notes

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class NotesDataRepositoryTest : ShouldSpec({
    val userService = mockk<UserService>()
    val repository = NotesDataRepository(userService)

    should("return user notes from repository") {
        val entityUser = User(
            userId = 610L,
            firstName = "Notes",
            lastName = null,
            userName = "notes"
        ).apply {
            notes.addAll(listOf("one", "two"))
        }
        every { userService.getUser(610L) } returns entityUser
        val tgUser = mockk<TgUser>(relaxed = true) {
            every { id } returns 610L
        }

        val dto = repository.getData(tgUser)

        dto.notes shouldContainExactly listOf("one", "two")
    }
})
