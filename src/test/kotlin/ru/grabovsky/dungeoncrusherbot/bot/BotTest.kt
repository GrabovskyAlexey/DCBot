package ru.grabovsky.dungeoncrusherbot.bot

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.dungeoncrusherbot.config.BotConfig
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ReceiverService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.commands.AbstractFlowCommand
import ru.grabovsky.dungeoncrusherbot.strategy.commands.Command
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowEngine
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys

class BotTest : ShouldSpec({

    val telegramClient = mockk<TelegramClient>(relaxed = true)
    val receiverService = mockk<ReceiverService>(relaxed = true)
    val userService = mockk<UserService>(relaxed = true)
    val flowEngine = mockk<FlowEngine>(relaxed = true)

    val firstCommand = object : AbstractFlowCommand(Command.HELP, FlowKeys.HELP, userService, flowEngine) {}
    val secondCommand = object : AbstractFlowCommand(Command.START, FlowKeys.START, userService, flowEngine) {}

    should("set bot commands sorted by order and expose token") {
        val commandSlot = slot<SetMyCommands>()
        val config = BotConfig(token = "token", name = "botName")
        val commands = listOf(firstCommand, secondCommand)

        every { telegramClient.execute(capture(commandSlot)) } returns true

        val bot = Bot(config, telegramClient, receiverService, commands)

        bot.botToken shouldBe "token"
        bot.updatesConsumer shouldBe bot

        val sentCommands = commandSlot.captured.commands
        sentCommands.shouldContainExactly(
            BotCommand(secondCommand.commandIdentifier, secondCommand.description),
            BotCommand(firstCommand.commandIdentifier, firstCommand.description)
        )
    }

    should("delegate non-command updates to receiver service") {
        val config = BotConfig(token = "t", name = "name")
        val update = mockk<Update>()
        val commands = listOf(secondCommand)

        every { telegramClient.execute(any<SetMyCommands>()) } returns true

        val bot = Bot(config, telegramClient, receiverService, commands)
        bot.processNonCommandUpdate(update)

        verify { receiverService.execute(update) }
    }
})
