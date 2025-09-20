package ru.grabovsky.dungeoncrusherbot.service

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.dungeoncrusherbot.entity.NotifyHistory
import ru.grabovsky.dungeoncrusherbot.repository.NotifyHistoryRepository
import java.time.Instant

class NotifyHistoryServiceImplTest : ShouldSpec({

    val repository = mockk<NotifyHistoryRepository>()
    val service = NotifyHistoryServiceImpl(repository)

    beforeTest {
        clearMocks(repository)
    }

    should("save history via repository") {
        val history = NotifyHistory(messageId = 1, text = "msg", sendTime = Instant.now())
        every { repository.saveAndFlush(history) } returns history

        service.saveHistory(history)

        verify { repository.saveAndFlush(history) }
    }

    should("return events that are not deleted") {
        val histories = listOf(NotifyHistory(messageId = 2, text = "text", sendTime = Instant.now()))
        every { repository.findNotifyHistoryByDeletedNot(true) } returns histories

        service.getNotDeletedHistoryEvent() shouldContainExactly histories
    }

    should("mark events as deleted and persist them") {
        val histories = mutableListOf(NotifyHistory(messageId = 3, text = "text", sendTime = Instant.now()))
        every { repository.saveAllAndFlush(histories) } returns histories

        service.markAsDeleted(histories)

        histories.first().deleted.shouldBeTrue()
        verify { repository.saveAllAndFlush(histories) }
    }

    should("remove old events from repository") {
        val histories = listOf(NotifyHistory(messageId = 4, text = "old", sendTime = Instant.now()))
        every { repository.findNotifyHistoryBySendTimeBefore(any()) } returns histories
        every { repository.deleteAll(histories) } returns Unit

        service.deleteOldEvents()

        verify { repository.deleteAll(histories) }
    }
})
