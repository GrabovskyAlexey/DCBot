package ru.grabovsky.dungeoncrusherbot.strategy.message

import io.kotest.core.spec.style.ShouldSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.*
import ru.grabovsky.dungeoncrusherbot.strategy.message.exchange.ExchangeDetailMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.exchange.ExchangeMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.exchange.UpdateExchangeDetailMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.exchange.UpdateExchangeMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.maze.MazeMessage
import ru.grabovsky.dungeoncrusherbot.strategy.message.maze.UpdateMazeMessage
import java.util.*

class MessageDelegationTest : ShouldSpec({
    val messageService = mockk<MessageGenerateService>(relaxed = true)

    fun <T : DataModel> assertDelegation(message: AbstractSendMessage<T>, data: T) {
        val user = mockk<User>(relaxed = true)
        message.message(user, Locale.forLanguageTag("ru"), data)
        verify { messageService.process(message.classStateCode(), data, any()) }
    }

    beforeTest {
        clearMocks(messageService)
        every { messageService.process(any(), any(), any()) } returns "generated"
    }

    should("delegate generation for maze messages") {
        val dto = MazeDto(sameSteps = false)
        assertDelegation(MazeMessage(messageService), dto)
        assertDelegation(UpdateMazeMessage(messageService), dto)
    }

    should("delegate generation for exchange messages") {
        val listDto = ExchangeDto(
            servers = listOf(
                ExchangeDto.Server(id = 1, main = false, hasRequests = false),
                ExchangeDto.Server(id = 2, main = true, hasRequests = true)
            ),
            username = "tester"
        )
        val detailDto = ExchangeDetailDto(
            username = "tester",
            serverId = 1,
            requests = emptyList(),
        )
        assertDelegation(ExchangeMessage(messageService), listDto)
        assertDelegation(UpdateExchangeMessage(messageService), listDto)
        assertDelegation(ExchangeDetailMessage(messageService), detailDto)
        assertDelegation(UpdateExchangeDetailMessage(messageService), detailDto)
    }
})

