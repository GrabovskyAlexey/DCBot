package ru.grabovsky.dungeoncrusherbot.strategy.processor.message

import io.kotest.core.spec.style.ShouldSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import org.telegram.telegrambots.meta.api.objects.User as TgUser
import org.telegram.telegrambots.meta.api.objects.message.Message

class SendReportProcessorTest : ShouldSpec({
    val userService = mockk<UserService>(relaxed = true)
    val processor = SendReportProcessor(userService)

    should("перенаправлять сообщение администратору") {
        val user = mockk<TgUser>(relaxed = true)
        val message = mockk<Message>(relaxed = true) { every { text } returns "report" }

        processor.execute(user, message)

        verify { userService.sendAdminMessage(user, "report") }
    }
})

