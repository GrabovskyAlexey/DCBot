package ru.grabovsky.dungeoncrusherbot.strategy.data

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class StartDataRepositoryTest : ShouldSpec({
    val repository = StartDataRepository()

    should("возвращать username если он есть") {
        val tgUser = mockk<TgUser>()
        every { tgUser.userName } returns "tester"
        every { tgUser.firstName } returns "Tester"

        repository.getData(tgUser).username shouldBe "tester"
    }

    should("использовать firstName если username отсутствует") {
        val tgUser = mockk<TgUser>()
        every { tgUser.userName } returns null
        every { tgUser.firstName } returns "Fallback"

        repository.getData(tgUser).username shouldBe "Fallback"
    }
})
