package ru.grabovsky.dungeoncrusherbot.strategy.commands

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.entity.UserProfile
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowEngine
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class CommandsTest : ShouldSpec({
    val telegramClient = mockk<TelegramClient>(relaxed = true)
    val chat = mockk<Chat>(relaxed = true)

    should("create or update user and start flow for StartCommand") {
        val userService = mockk<UserService>()
        val engine = mockk<FlowEngine>(relaxed = true)
        val command = StartCommand(userService, engine)
        val tgUser = mockk<TgUser>(relaxed = true) { every { id } returns 100L }
        val persisted = User(100L, "Tester", null, "tester").apply { profile = UserProfile(userId = userId, user = this) }
        every { userService.createOrUpdateUser(tgUser) } returns persisted
        every { userService.getUser(100L) } returns persisted
        every { engine.start(FlowKeys.START, tgUser, any()) } returns true

        command.execute(telegramClient, tgUser, chat, emptyArray())

        verify { userService.createOrUpdateUser(tgUser) }
        verify { engine.start(FlowKeys.START, tgUser, any()) }
    }

    should("start flow when executing MazeCommand") {
        val userService = mockk<UserService>()
        val engine = mockk<FlowEngine>(relaxed = true)
        val command = MazeCommand(userService, engine)
        val tgUser = mockk<TgUser>(relaxed = true) { every { id } returns 150L }
        val persisted = User(150L, "Maze", null, "maze").apply { profile = UserProfile(userId = userId, user = this) }
        every { userService.createOrUpdateUser(tgUser) } returns persisted
        every { userService.getUser(150L) } returns persisted
        every { engine.start(FlowKeys.MAZE, tgUser, any()) } returns true

        command.execute(telegramClient, tgUser, chat, emptyArray())

        verify { userService.createOrUpdateUser(tgUser) }
        verify { engine.start(FlowKeys.MAZE, tgUser, any()) }
    }

    should("skip legacy resources initialization for ResourcesCommand") {
        val userService = mockk<UserService>()
        val flowEngine = mockk<FlowEngine>(relaxed = true)
        val command = ResourcesCommand(userService, flowEngine)
        val tgUser = mockk<TgUser>(relaxed = true) {
            every { id } returns 200L
            every { userName } returns "tester"
            every { firstName } returns "Tester"
        }
        val entityUser = User(200L, "Tester", null, "tester").apply { profile = UserProfile(userId = userId, user = this) }
        every { userService.createOrUpdateUser(tgUser) } returns entityUser
        every { userService.getUser(200L) } returns entityUser

        command.prepare(tgUser, chat, emptyArray())

        verify(exactly = 0) { userService.saveUser(any()) }
    }

    should("avoid persistence in ResourcesCommand prepare") {
        val userService = mockk<UserService>()
        val flowEngine = mockk<FlowEngine>(relaxed = true)
        val command = ResourcesCommand(userService, flowEngine)
        val tgUser = mockk<TgUser>(relaxed = true) {
            every { id } returns 201L
            every { userName } returns "tester"
            every { firstName } returns "Tester"
        }
        val entityUser = User(201L, "Tester", null, "tester").apply {
            profile = UserProfile(userId = userId, user = this)
        }
        every { userService.createOrUpdateUser(tgUser) } returns entityUser
        every { userService.getUser(201L) } returns entityUser

        command.prepare(tgUser, chat, emptyArray())

        verify(exactly = 0) { userService.saveUser(any()) }
    }

    should("throw if user not found for ResourcesCommand") {
        val userService = mockk<UserService>()
        val flowEngine = mockk<FlowEngine>(relaxed = true)
        val command = ResourcesCommand(userService, flowEngine)
        val tgUser = mockk<TgUser>(relaxed = true) {
            every { id } returns 202L
            every { userName } returns "tester"
            every { firstName } returns "Tester"
        }
        every { userService.createOrUpdateUser(tgUser) } returns User(202L, "Tester", null, "tester").apply { profile = UserProfile(userId = userId, user = this) }
        every { userService.getUser(202L) } returns null

        shouldThrow<EntityNotFoundException> {
            command.prepare(tgUser, chat, emptyArray())
        }
    }
})
