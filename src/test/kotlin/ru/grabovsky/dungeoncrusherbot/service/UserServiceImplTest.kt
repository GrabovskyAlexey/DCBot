package ru.grabovsky.dungeoncrusherbot.service

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.*
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.entity.UserProfile
import ru.grabovsky.dungeoncrusherbot.repository.AdminMessageRepository
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.FlowStateService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.admin.AdminMessageFlow
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowActionExecutor
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowPayloadSerializer
import java.time.Instant
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class UserServiceImplTest : ShouldSpec({
    val userRepository = mockk<UserRepository>()
    val adminMessageRepository = mockk<AdminMessageRepository>(relaxed = true)
    val flowStateService = mockk<FlowStateService>(relaxed = true)
    val payloadSerializer = mockk<FlowPayloadSerializer>(relaxed = true)
    val adminMessageFlow = mockk<AdminMessageFlow>(relaxed = true)
    val actionExecutor = mockk<FlowActionExecutor>(relaxed = true)
    val service = UserServiceImpl(
        userRepository,
        adminMessageRepository,
        flowStateService,
        payloadSerializer,
        adminMessageFlow,
        actionExecutor
    )

    beforeTest {
        clearMocks(userRepository)
    }

    should("create new user with language and last action time") {
        val tgUser = mockk<TgUser>()
        every { tgUser.id } returns 77L
        every { tgUser.firstName } returns "Jane"
        every { tgUser.lastName } returns "Doe"
        every { tgUser.userName } returns "jane"
        every { tgUser.languageCode } returns "en"

        every { userRepository.findUserByUserId(77L) } returns null
        val captured = slot<User>()
        every { userRepository.saveAndFlush(capture(captured)) } answers { captured.captured }

        val result = service.createOrUpdateUser(tgUser)

        val saved = captured.captured
        saved.language shouldBe "en"
        saved.lastActionAt.shouldNotBeNull()
        saved.lastActionAt!!.isAfter(Instant.now().minusSeconds(5)) shouldBe true
        saved.profile!!.isBlocked shouldBe false
        result shouldBe saved
        verify(exactly = 1) { userRepository.saveAndFlush(any()) }
    }

    should("update stored user with language and interaction time") {
        val existingUser = User(
            userId = 88L,
            firstName = "Old",
            lastName = "Name",
            userName = "old",
            language = "ru"
        ).apply {
            profile = UserProfile(userId = userId, user = this)
        }
        existingUser.lastActionAt = null
        existingUser.profile!!.isBlocked = true

        val tgUser = mockk<TgUser>()
        every { tgUser.id } returns 88L
        every { tgUser.firstName } returns "New"
        every { tgUser.lastName } returns "Name"
        every { tgUser.userName } returns "new"
        every { tgUser.languageCode } returns "en"

        every { userRepository.findUserByUserId(88L) } returns existingUser
        every { userRepository.saveAndFlush(existingUser) } returns existingUser

        val result = service.createOrUpdateUser(tgUser)

        existingUser.firstName shouldBe "New"
        existingUser.lastName shouldBe "Name"
        existingUser.userName shouldBe "new"
        existingUser.language shouldBe "en"
        existingUser.profile!!.isBlocked shouldBe false
        existingUser.lastActionAt.shouldNotBeNull()
        result shouldBe existingUser
        verify(exactly = 1) { userRepository.saveAndFlush(existingUser) }
    }

})
