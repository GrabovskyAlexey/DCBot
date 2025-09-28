package ru.grabovsky.dungeoncrusherbot.strategy.state.notes

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.entity.UserState
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class NotesStateTest : ShouldSpec({
    val user = mockk<TgUser>(relaxed = true) { every { id } returns 801L }

    should("route next state according to callback data") {
        val stateService = mockk<StateService>()
        val state = UserState(userId = 801L, state = StateCode.NOTES)
        every { stateService.getState(user) } returns state

        val notesState = NotesState(stateService)

        state.callbackData = "ADD_NOTE"
        notesState.getNextState(user) shouldBe StateCode.ADD_NOTE

        state.callbackData = "REMOVE_NOTE"
        notesState.getNextState(user) shouldBe StateCode.REMOVE_NOTE

        state.callbackData = null
        notesState.getNextState(user) shouldBe StateCode.UPDATE_NOTES
    }

    should("return VERIFY for add and remove note states") {
        AddNoteState().getNextState(user) shouldBe StateCode.VERIFY
        RemoveNoteState().getNextState(user) shouldBe StateCode.VERIFY
    }
})
