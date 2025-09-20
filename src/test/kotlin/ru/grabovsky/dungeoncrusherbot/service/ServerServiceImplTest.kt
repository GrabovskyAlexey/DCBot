package ru.grabovsky.dungeoncrusherbot.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityNotFoundException
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.repository.ServerRepository

class ServerServiceImplTest : ShouldSpec({
    val repository = mockk<ServerRepository>()
    val service = ServerServiceImpl(repository)

    should("возвращать сервер по идентификатору") {
        val server = Server(7, "Seven")
        every { repository.findServerById(7) } returns server

        service.getServerById(7) shouldBe server
    }

    should("бросать исключение если сервер не найден") {
        every { repository.findServerById(99) } returns null
        shouldThrow<EntityNotFoundException> { service.getServerById(99) }
    }

    should("возвращать все сервера") {
        val servers = listOf(Server(1, "One"))
        every { repository.findAll() } returns servers
        service.getAllServers() shouldBe servers
    }
})
