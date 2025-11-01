package ru.grabovsky.dungeoncrusherbot.framework

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import ru.grabovsky.dungeoncrusherbot.entity.UpdateMessage
import ru.grabovsky.dungeoncrusherbot.entity.User
import ru.grabovsky.dungeoncrusherbot.entity.UserProfile
import ru.grabovsky.dungeoncrusherbot.repository.UpdateMessageRepository
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.TelegramBotService

class RunAfterStartupTest : ShouldSpec({

    val telegramBotService = mockk<TelegramBotService>()
    val userRepository = mockk<UserRepository>()
    val updateMessageRepository = mockk<UpdateMessageRepository>()

    val runner = RunAfterStartup(telegramBotService, userRepository, updateMessageRepository)

    beforeTest {
        clearMocks(telegramBotService, userRepository, updateMessageRepository)
    }

    should("skip processing when there are no updates") {
        every { updateMessageRepository.findUpdateMessagesBySentNot() } returns emptyList()

        runner.runAfterStartup()

        verify(exactly = 0) { userRepository.findAllNotBlockedUser() }
    }

    should("send release notes and mark failed users as blocked") {
        val update = UpdateMessage(version = "1.0", text = "note", sent = false)
        val userOk = User(1L, "Ok", null, "ok").apply { profile = UserProfile(userId = userId, user = this) }
        val userFail = User(2L, "Fail", null, "fail").apply { profile = UserProfile(userId = userId, user = this) }

        every { updateMessageRepository.findUpdateMessagesBySentNot() } returns listOf(update)
        every { userRepository.findAllNotBlockedUser() } returns listOf(userOk, userFail)
        justRun { telegramBotService.sendReleaseNotes(userOk, update) }
        val exception = mockk<TelegramApiRequestException>(relaxed = true) {
            every { errorCode } returns 403
        }
        every { telegramBotService.sendReleaseNotes(userFail, update) } throws exception
        every { userRepository.save(userFail) } returns userFail
        every { updateMessageRepository.saveAll(listOf(update)) } returns listOf(update)

        runner.runAfterStartup()

        userFail.profile?.isBlocked shouldBe true
        verify { telegramBotService.sendReleaseNotes(userOk, update) }
        verify { telegramBotService.sendReleaseNotes(userFail, update) }
        verify { userRepository.save(userFail) }
        update.sent shouldBe true
        verify { updateMessageRepository.saveAll(listOf(update)) }
    }
})
