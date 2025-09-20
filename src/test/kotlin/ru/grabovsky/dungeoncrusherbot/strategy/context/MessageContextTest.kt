package ru.grabovsky.dungeoncrusherbot.strategy.context

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.dto.MessageModelDto
import ru.grabovsky.dungeoncrusherbot.dto.ReplyMarkupDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.data.AbstractDataRepository
import ru.grabovsky.dungeoncrusherbot.strategy.dto.DataModel
import ru.grabovsky.dungeoncrusherbot.strategy.message.AbstractSendMessage
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

class MessageContextTest : ShouldSpec({

    val messageService = mockk<MessageGenerateService>()
    every { messageService.process(any(), any()) } returns "generated"

    class StartMessage(messageGenerateService: MessageGenerateService) : AbstractSendMessage<DataModel>(messageGenerateService) {
        override fun inlineButtons(user: User, data: DataModel?) =
            listOf(InlineMarkupDataDto(rowPos = 0, text = "inline", data = CallbackObject(StateCode.START, "payload")))

        override fun replyButtons(user: User, data: DataModel?) =
            listOf(ReplyMarkupDto(requestLocation = false, text = "reply"))
    }

    class HelpMessage(messageGenerateService: MessageGenerateService) : AbstractSendMessage<DataModel>(messageGenerateService) {
        override fun isPermitted(user: User) = false
    }

    class StartDataRepository(private val model: DataModel) : AbstractDataRepository<DataModel>() {
        override fun getData(user: User): DataModel = model
    }

    val dataModel = object : DataModel {}
    val sendMessage = StartMessage(messageService)
    val deniedMessage = HelpMessage(messageService)
    val dataRepository = StartDataRepository(dataModel)

    val context = MessageContext(
        sendMessages = mapOf(StateCode.START to sendMessage, StateCode.HELP to deniedMessage),
        abstractDataRepository = listOf(dataRepository)
    )

    val user = mockk<User>(relaxed = true)

    should("provide message model for permitted state") {
        val model: MessageModelDto = context.getMessage(user, StateCode.START)!!
        model.message shouldBe "generated"
        model.inlineButtons.size shouldBe 1
        model.replyButtons.size shouldBe 1
    }

    should("return null when send message is not permitted") {
        context.getMessage(user, StateCode.HELP) shouldBe null
    }
})
