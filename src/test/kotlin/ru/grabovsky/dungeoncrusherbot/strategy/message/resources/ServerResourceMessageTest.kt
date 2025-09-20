package ru.grabovsky.dungeoncrusherbot.strategy.message.resources

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.dto.CallbackObject
import ru.grabovsky.dungeoncrusherbot.dto.InlineMarkupDataDto
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MessageGenerateService
import ru.grabovsky.dungeoncrusherbot.strategy.dto.ServerResourceDto
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class ServerResourceMessageTest : ShouldSpec({
    val messageService = mockk<MessageGenerateService>(relaxed = true)
    val message = ServerResourceMessage(messageService)

    should("строить полный набор кнопок для неосновного сервера без главного") {
        val dto = ServerResourceDto(
            id = 5,
            draadorCount = 10,
            voidCount = 2,
            balance = 3,
            exchange = "123",
            history = listOf("item"),
            hasHistory = true,
            notifyDisable = false,
            main = false,
            cbEnabled = true,
            quickResourceEnabled = true,
            cbCount = 1,
            notes = emptyList(),
            hasMain = false,
        )
        val user = mockk<TgUser>(relaxed = true)

        val buttons = message.inlineButtons(user, dto)

        buttons.shouldNotBeEmpty()
        val callbacks = buttons.map(InlineMarkupDataDto::data)
        callbacks.shouldContainAll(
            CallbackObject(StateCode.SERVER_RESOURCE, "ADD_DRAADOR"),
            CallbackObject(StateCode.SERVER_RESOURCE, "SELL_DRAADOR"),
            CallbackObject(StateCode.SERVER_RESOURCE, "RECEIVE_DRAADOR"),
            CallbackObject(StateCode.SERVER_RESOURCE, "SEND_DRAADOR"),
            CallbackObject(StateCode.SERVER_RESOURCE, "ADD_VOID"),
            CallbackObject(StateCode.SERVER_RESOURCE, "REMOVE_VOID"),
            CallbackObject(StateCode.SERVER_RESOURCE, "ADD_CB"),
            CallbackObject(StateCode.SERVER_RESOURCE, "REMOVE_CB"),
            CallbackObject(StateCode.SERVER_RESOURCE, "REMOVE_EXCHANGE"),
            CallbackObject(StateCode.SERVER_RESOURCE, "ADD_EXCHANGE"),
            CallbackObject(StateCode.SERVER_RESOURCE, "SET_MAIN"),
            CallbackObject(StateCode.SERVER_RESOURCE, "DISABLE_NOTIFY"),
            CallbackObject(StateCode.SERVER_RESOURCE, "RESOURCE_HISTORY"),
            CallbackObject(StateCode.SERVER_RESOURCE, "BACK"),
        )
        callbacks.map(CallbackObject::state).shouldContain(StateCode.INCREMENT_DRAADOR)
        callbacks.map(CallbackObject::state).shouldContain(StateCode.DECREMENT_DRAADOR)
        callbacks.map(CallbackObject::state).shouldContain(StateCode.INCREMENT_VOID)
        callbacks.map(CallbackObject::state).shouldContain(StateCode.DECREMENT_VOID)
        callbacks.map(CallbackObject::state).shouldContain(StateCode.INCREMENT_CB)
        callbacks.map(CallbackObject::state).shouldContain(StateCode.DECREMENT_CB)
    }

    should("не выводить кнопки отправки для основного сервера и показывать блок заметок") {
        val dto = ServerResourceDto(
            id = 8,
            draadorCount = 5,
            voidCount = 1,
            balance = 0,
            exchange = null,
            history = null,
            hasHistory = false,
            notifyDisable = true,
            main = true,
            cbEnabled = true,
            quickResourceEnabled = false,
            cbCount = 2,
            notes = listOf("note"),
            hasMain = true,
        )
        val user = mockk<TgUser>(relaxed = true)

        val buttons = message.inlineButtons(user, dto)

        val callbacks = buttons.map(InlineMarkupDataDto::data)
        callbacks.shouldContain(CallbackObject(StateCode.SERVER_RESOURCE, "ADD_NOTE"))
        callbacks.shouldContain(CallbackObject(StateCode.SERVER_RESOURCE, "REMOVE_NOTE"))
        callbacks.shouldContain(CallbackObject(StateCode.SERVER_RESOURCE, "REMOVE_MAIN"))
        callbacks.shouldContain(CallbackObject(StateCode.SERVER_RESOURCE, "BACK"))
        callbacks.shouldNotContain(CallbackObject(StateCode.SERVER_RESOURCE, "RECEIVE_DRAADOR"))
        callbacks.shouldNotContain(CallbackObject(StateCode.SERVER_RESOURCE, "SEND_DRAADOR"))
        callbacks.map(CallbackObject::state).shouldNotContain(StateCode.INCREMENT_DRAADOR)
        callbacks.map(CallbackObject::state).shouldNotContain(StateCode.INCREMENT_CB)
        callbacks.map(CallbackObject::state).shouldNotContain(StateCode.INCREMENT_VOID)
    }
})
