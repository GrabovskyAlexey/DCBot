package ru.grabovsky.dungeoncrusherbot.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.entity.*
import ru.grabovsky.dungeoncrusherbot.service.interfaces.AdjustType
import ru.grabovsky.dungeoncrusherbot.service.interfaces.GoogleFormService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ResourceOperation
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository
import java.time.LocalDate
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class ResourcesServiceImplTest : ShouldSpec({

    lateinit var userService: FakeUserService
    lateinit var googleFormService: FakeGoogleFormService
    lateinit var service: ResourcesServiceImpl
    lateinit var userRepository: UserRepository
    lateinit var resourceStateSyncService: ResourceStateSyncService
    lateinit var telegramUser: TgUser
    lateinit var entityUser: User
    lateinit var resources: Resources
    lateinit var serverData: ServerResourceData

    fun prepareUser(serverId: Int = 5) {
        entityUser = User(userId = 77L, firstName = "Tester", lastName = null, userName = "tester").apply {
            profile = UserProfile(userId = userId, user = this)
        }
        resources = Resources(user = entityUser)
        entityUser.resources = resources
        resources.data.servers[serverId] = ServerResourceData().also { serverData = it }
        resources.history[serverId] = mutableListOf()
        userService.users[entityUser.userId] = entityUser
    }

    beforeTest {
        userService = FakeUserService()
        googleFormService = FakeGoogleFormService()
        userRepository = mockk(relaxed = true)
        resourceStateSyncService = mockk(relaxed = true)
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

    should("throw when resources are missing") {
        entityUser.resources = null

        shouldThrow<IllegalStateException> {
            service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.ADD_VOID, 1))
        }
    }

    should("increase and decrease void count with history") {
        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.ADD_VOID, 3))

        serverData.voidCount shouldBe 3
        resources.history[5]!!.last().apply {
            quantity shouldBe 3
            type shouldBe DirectionType.ADD
        }

        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.REMOVE_VOID, 2))
        serverData.voidCount shouldBe 1
        resources.history[5]!!.last().type shouldBe DirectionType.REMOVE
    }

    should("increase and decrease CB count with history") {
        entityUser.profile!!.settings = entityUser.profile!!.settings.copy(resourcesCb = true)

        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.ADD_CB, 4))
        serverData.cbCount shouldBe 4

        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.REMOVE_CB, 1))
        serverData.cbCount shouldBe 3
    }

    should("process draador operations") {
        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.ADD_DRAADOR, 4))
        serverData.draadorCount shouldBe 4
        resources.history[5]!!.last().type shouldBe DirectionType.CATCH

        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.SELL_DRAADOR, 3))
        serverData.draadorCount shouldBe 1
        resources.history[5]!!.last().type shouldBe DirectionType.TRADE

        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.SEND_DRAADOR, 5))
        serverData.draadorCount shouldBe 0
        serverData.balance shouldBe 5
        resources.history[5]!!.last().type shouldBe DirectionType.OUTGOING
    }

    should("prevent negative draador count") {
        serverData.draadorCount = 2

        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.SELL_DRAADOR, 5))

        serverData.draadorCount shouldBe 0
    }

    should("handle receive operation and transfer to main server") {
        entityUser.profile!!.mainServerId = 1
        resources.data.servers[1] = ServerResourceData()
        resources.history[1] = mutableListOf()

        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.RECEIVE_DRAADOR, 4))

        serverData.balance shouldBe -4
        resources.history[5]!!.last().type shouldBe DirectionType.INCOMING
        resources.data.servers[1]!!.draadorCount shouldBe 4
        resources.history[1]!!.last().apply {
            type shouldBe DirectionType.INCOMING
            fromServer shouldBe 5
        }
    }

    should("update exchange value") {
        service.applyOperation(telegramUser, 5, ResourceOperation.SetExchange("some"))
        serverData.exchange shouldBe "some"

        service.applyOperation(telegramUser, 5, ResourceOperation.ClearExchange)
        serverData.exchange shouldBe null
    }

    should("toggle notify flag") {
        serverData.notifyDisable shouldBe false
        service.applyOperation(telegramUser, 5, ResourceOperation.ToggleNotify)
        serverData.notifyDisable shouldBe true
    }

    should("set and remove main server") {
        service.applyOperation(telegramUser, 5, ResourceOperation.MarkMain)
        entityUser.profile!!.mainServerId shouldBe 5

        service.applyOperation(telegramUser, 5, ResourceOperation.UnmarkMain)
        entityUser.profile!!.mainServerId shouldBe null
    }

    should("limit history to 20 records") {
        val history = resources.history[5]!!
        repeat(20) { index ->
            history.add(
                ResourcesHistory(
                    LocalDate.now(),
                    ru.grabovsky.dungeoncrusherbot.entity.ResourceType.VOID,
                    DirectionType.ADD,
                    index + 1
                )
            )
        }

        service.applyOperation(telegramUser, 5, ResourceOperation.Adjust(AdjustType.ADD_VOID, 1))

        history.shouldHaveSize(20)
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
})

private class FakeUserService : UserService {
    val users: MutableMap<Long, User> = mutableMapOf()

    override fun createOrUpdateUser(user: TgUser): User =
        throw UnsupportedOperationException("Not used in tests")

    override fun saveUser(user: User) {
        users[user.userId] = user
    }

    override fun getUser(userId: Long): User? = users[userId]

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
