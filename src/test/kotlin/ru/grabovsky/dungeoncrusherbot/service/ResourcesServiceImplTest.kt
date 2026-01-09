package ru.grabovsky.dungeoncrusherbot.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.entity.*
import ru.grabovsky.dungeoncrusherbot.repository.ResourceServerHistoryRepository
import ru.grabovsky.dungeoncrusherbot.repository.ResourceServerStateRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.AdjustType
import ru.grabovsky.dungeoncrusherbot.service.interfaces.GoogleFormService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ResourceOperation
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class ResourcesServiceImplTest : ShouldSpec({

    lateinit var userService: FakeUserService
    lateinit var googleFormService: FakeGoogleFormService
    lateinit var service: ResourcesServiceImpl
    lateinit var userRepository: UserRepository
    lateinit var resourceStateSyncService: ResourceStateSyncService
    lateinit var resourceServerStateRepository: ResourceServerStateRepository
    lateinit var resourceServerHistoryRepository: ResourceServerHistoryRepository
    lateinit var serverService: ServerService
    lateinit var telegramUser: TgUser
    lateinit var entityUser: User
    val stateIdCounter = AtomicLong(1)
    val historyIdCounter = AtomicLong(1)
    val stateStore = mutableMapOf<Pair<Long, Int>, ResourceServerState>()
    val historyStore = mutableMapOf<Long, MutableList<ResourceServerHistory>>()

    fun prepareUser(serverId: Int = 5) {
        entityUser = User(userId = 77L, firstName = "Tester", lastName = null, userName = "tester").apply {
            profile = UserProfile(userId = userId, user = this)
        }
        userService.users[entityUser.userId] = entityUser
    }

    fun stubResourceRepositories() {
        stubResourceRepositories(
            resourceServerStateRepository = resourceServerStateRepository,
            resourceServerHistoryRepository = resourceServerHistoryRepository,
            stateStore = stateStore,
            historyStore = historyStore,
            stateIdCounter = stateIdCounter,
            historyIdCounter = historyIdCounter
        )
    }

    fun requireState(serverId: Int): ResourceServerState =
        requireState(resourceStateSyncService, entityUser, serverId)

    fun getHistory(serverId: Int): List<ResourcesHistory> =
        getHistory(resourceStateSyncService, entityUser, serverId)

    beforeTest {
        userService = FakeUserService()
        googleFormService = FakeGoogleFormService()
        userRepository = mockk(relaxed = true)
        stateStore.clear()
        historyStore.clear()
        stateIdCounter.set(1)
        historyIdCounter.set(1)
        resourceServerStateRepository = mockk(relaxed = true)
        resourceServerHistoryRepository = mockk(relaxed = true)
        serverService = FakeServerService()
        resourceStateSyncService = ResourceStateSyncService(
            resourceServerStateRepository,
            resourceServerHistoryRepository,
            serverService
        )
        stubResourceRepositories()
        service = ResourcesServiceImpl(
            userService = userService,
            googleFormService = googleFormService,
            resourceStateSyncService = resourceStateSyncService,
            userRepository = userRepository
        )
        telegramUser = TgUser.builder()
            .id(77L)
            .firstName("TG")
            .isBot(false)
            .build()
        prepareUser()
    }

    should("throw when user is not found") {
        userService.users.clear()

        shouldThrow<jakarta.persistence.EntityNotFoundException> {
            service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.ADD_VOID, 1))
        }
    }

    should("increase and decrease void count with history") {
        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.ADD_VOID, 3))

        val state = requireState(5)
        state.voidCount shouldBe 3
        getHistory(5).last().apply {
            quantity shouldBe 3
            type shouldBe DirectionType.ADD
        }

        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.REMOVE_VOID, 2))
        state.voidCount shouldBe 1
        getHistory(5).last().type shouldBe DirectionType.REMOVE
    }

    should("increase and decrease CB count with history") {
        entityUser.profile!!.settings = entityUser.profile!!.settings.copy(resourcesCb = true)

        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.ADD_CB, 4))
        requireState(5).cbCount shouldBe 4

        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.REMOVE_CB, 1))
        requireState(5).cbCount shouldBe 3
    }

    should("process draador operations") {
        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.ADD_DRAADOR, 4))
        requireState(5).draadorCount shouldBe 4
        getHistory(5).last().type shouldBe DirectionType.CATCH

        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.SELL_DRAADOR, 3))
        requireState(5).draadorCount shouldBe 1
        getHistory(5).last().type shouldBe DirectionType.TRADE

        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.SEND_DRAADOR, 5))
        requireState(5).draadorCount shouldBe 0
        requireState(5).balance shouldBe 5
        getHistory(5).last().type shouldBe DirectionType.OUTGOING
    }

    should("prevent negative draador count") {
        val state = resourceStateSyncService.getOrCreateState(entityUser, 5)
        state.draadorCount = 2
        resourceStateSyncService.saveState(state)

        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.SELL_DRAADOR, 5))

        requireState(5).draadorCount shouldBe 0
    }

    should("handle receive operation and transfer to main server") {
        entityUser.profile!!.mainServerId = 1

        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.RECEIVE_DRAADOR, 4))

        requireState(5).balance shouldBe -4
        getHistory(5).last().type shouldBe DirectionType.INCOMING
        requireState(1).draadorCount shouldBe 4
        getHistory(1).last().apply {
            type shouldBe DirectionType.INCOMING
            fromServer shouldBe 5
        }
    }

    should("update exchange value") {
        service.applyOperation(telegramUser, 5, ResourceOperation.SetExchange("some"))
        requireState(5).exchangeLabel shouldBe "some"

        service.applyOperation(telegramUser, 5, ResourceOperation.ClearExchange)
        requireState(5).exchangeLabel shouldBe null
    }

    should("toggle notify flag") {
        val state = resourceStateSyncService.getOrCreateState(entityUser, 5)
        state.notifyDisable shouldBe false
        resourceStateSyncService.saveState(state)
        service.applyOperation(telegramUser, 5, ResourceOperation.ToggleNotify)
        requireState(5).notifyDisable shouldBe true
    }

    should("set and remove main server") {
        service.applyOperation(telegramUser, 5, ResourceOperation.MarkMain)
        entityUser.profile!!.mainServerId shouldBe 5

        service.applyOperation(telegramUser, 5, ResourceOperation.UnmarkMain)
        entityUser.profile!!.mainServerId shouldBe null
    }

    should("limit history to 20 records") {
        val state = resourceStateSyncService.getOrCreateState(entityUser, 5)
        repeat(20) { index ->
            resourceStateSyncService.appendHistory(
                state,
                ResourcesHistory(
                    LocalDate.now(),
                    ru.grabovsky.dungeoncrusherbot.entity.ResourceType.VOID,
                    DirectionType.ADD,
                    index + 1,
                    prevVoidCount = index
                )
            )
        }

        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.ADD_VOID, 1))

        resourceStateSyncService.getHistoryEntries(state.id!!).shouldHaveSize(20)
    }

    should("send watermelon report when criteria are met") {
        prepareUser(serverId = 8)
        entityUser.profile!!.settings = UserSettings(
            sendWatermelon = true,
            discordUsername = "discord",
            resourcesCb = false,
            resourcesQuickChange = false
        )

        service.applyOperation(telegramUser, 8, ResourceOperation.Adjust(AdjustType.ADD_DRAADOR, 9))

        googleFormService.calls shouldBe listOf("9" to "discord")
    }

    should("skip watermelon report when criteria are not met") {
        prepareUser(serverId = 8)
        entityUser.profile!!.settings = UserSettings(sendWatermelon = false, discordUsername = "discord")

        service.applyOperation(telegramUser, 8, ResourceOperation.Adjust(AdjustType.ADD_DRAADOR, 2))

        googleFormService.calls shouldBe emptyList()
    }

    should("restore previous balance and count when undo after clamp") {
        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.SEND_DRAADOR, 2))

        requireState(5).draadorCount shouldBe 0
        requireState(5).balance shouldBe 2

        val undone = service.undoLastOperation(telegramUser, 5)

        undone shouldBe true
        requireState(5).draadorCount shouldBe 0
        requireState(5).balance shouldBe 0
    }

})

private class FakeUserService : UserService {
    val users: MutableMap<Long, User> = mutableMapOf()

    override fun createOrUpdateUser(user: TgUser): User =
        throw UnsupportedOperationException("Not used in tests")

    override fun saveUser(user: User) {
        users[user.userId] = user
    }

    override fun getUser(userId: Long): User? = users[userId]

    override fun updateBlockedStatus(userId: Long, isBlocked: Boolean) {
        val user = users[userId] ?: return
        val profile = user.profile ?: return
        profile.isBlocked = isBlocked
    }

    override fun addNote(userId: Long, note: String): Boolean {
        val user = users[userId] ?: return false
        user.profile?.notes?.add(note)
        return true
    }

    override fun removeNote(userId: Long, index: Int): Boolean {
        val user = users[userId] ?: return false
        val position = index - 1
        val notes = user.profile?.notes ?: return false
        if (position !in notes.indices) {
            return false
        }
        notes.removeAt(position)
        return true
    }

    override fun clearNotes(user: TgUser) =
        throw UnsupportedOperationException("Not used in tests")

    override fun sendAdminMessage(user: TgUser, message: String, sourceMessageId: Int, sourceChatId: Long) =
        throw UnsupportedOperationException("Not used in tests")

    override fun sendAdminReply(admin: TgUser, targetUserId: Long, message: String, replyToMessageId: Int?) =
        throw UnsupportedOperationException("Not used in tests")

    override fun findByUsername(username: String): User? =
        users.values.firstOrNull { it.userName?.equals(username, ignoreCase = true) == true }
}

private class FakeGoogleFormService : GoogleFormService {
    val calls: MutableList<Pair<String, String>> = mutableListOf()

    override fun sendDraadorCount(count: String, discordName: String) {
        calls += count to discordName
    }
}

private class FakeServerService : ServerService {
    override fun getServerById(serverId: Int): Server =
        Server(id = serverId, name = "Server $serverId")

    override fun getAllServers(): List<Server> = emptyList()
}

private fun stubResourceRepositories(
    resourceServerStateRepository: ResourceServerStateRepository,
    resourceServerHistoryRepository: ResourceServerHistoryRepository,
    stateStore: MutableMap<Pair<Long, Int>, ResourceServerState>,
    historyStore: MutableMap<Long, MutableList<ResourceServerHistory>>,
    stateIdCounter: AtomicLong,
    historyIdCounter: AtomicLong
) {
    io.mockk.every { resourceServerStateRepository.findByUserUserIdAndServerId(any(), any()) } answers {
        val userId = invocation.args[0] as Long
        val serverId = invocation.args[1] as Int
        stateStore[userId to serverId]
    }
    io.mockk.every { resourceServerStateRepository.save(any()) } answers {
        val state = invocation.args[0] as ResourceServerState
        val saved = if (state.id == null) {
            state.copy(id = stateIdCounter.getAndIncrement())
        } else {
            state
        }
        stateStore[saved.user.userId to saved.server.id] = saved
        saved
    }
    io.mockk.every { resourceServerHistoryRepository.findTop20ByServerStateIdAndIsDeletedFalseOrderByIdDesc(any()) } answers {
        val stateId = invocation.args[0] as Long
        historyStore[stateId]
            ?.filter { !it.isDeleted }
            ?.sortedByDescending { it.id }
            ?.take(20)
            ?: emptyList()
    }
    io.mockk.every { resourceServerHistoryRepository.findAllByServerStateIdAndIsDeletedFalseOrderByIdAsc(any()) } answers {
        val stateId = invocation.args[0] as Long
        historyStore[stateId]
            ?.filter { !it.isDeleted }
            ?.sortedBy { it.id }
            ?: emptyList()
    }
    io.mockk.every { resourceServerHistoryRepository.save(any()) } answers {
        val entry = invocation.args[0] as ResourceServerHistory
        val saved = if (entry.id == null) {
            entry.copy(id = historyIdCounter.getAndIncrement())
        } else {
            entry
        }
        val list = historyStore.getOrPut(saved.serverState.id!!) { mutableListOf() }
        list += saved
        saved
    }
    io.mockk.every { resourceServerHistoryRepository.delete(any()) } answers {
        val entry = invocation.args[0] as ResourceServerHistory
        historyStore[entry.serverState.id!!]?.firstOrNull { it.id == entry.id }?.isDeleted = true
    }
}

private fun requireState(
    resourceStateSyncService: ResourceStateSyncService,
    user: User,
    serverId: Int
): ResourceServerState =
    resourceStateSyncService.findState(user.userId, serverId)
        ?: error("State not found for serverId=$serverId")

private fun getHistory(
    resourceStateSyncService: ResourceStateSyncService,
    user: User,
    serverId: Int
): List<ResourcesHistory> {
    val state = requireState(resourceStateSyncService, user, serverId)
    return resourceStateSyncService.getHistoryEntries(state.id!!).map {
        ResourcesHistory(
            date = it.eventDate,
            resource = it.resource,
            type = it.direction,
            quantity = it.quantity,
            fromServer = it.fromServer,
            prevDraadorCount = it.prevDraadorCount,
            prevVoidCount = it.prevVoidCount,
            prevCbCount = it.prevCbCount,
            prevBalance = it.prevBalance
        )
    }
}
