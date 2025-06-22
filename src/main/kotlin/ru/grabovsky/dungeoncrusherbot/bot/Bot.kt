package ru.grabovsky.dungeoncrusherbot.bot

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.telegram.telegrambots.extensions.bots.commandbot.CommandLongPollingTelegramBot
import org.telegram.telegrambots.longpolling.BotSession
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.dungeoncrusherbot.config.BotConfig
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ReceiverService
import ru.grabovsky.dungeoncrusherbot.strategy.commands.AbstractCommand


@Component
class Bot(
    private val config: BotConfig,
    private val client: TelegramClient,
    private val receiverService: ReceiverService,
    commands: List<AbstractCommand>
): SpringLongPollingBot, CommandLongPollingTelegramBot(client, true, { config.name }) {

    init {
        registerAll(*commands.toTypedArray())
        SetMyCommands(commands.map { BotCommand(it.commandIdentifier, it.description) })
            .also { client.execute(it) }
    }
    override fun getBotToken() = config.token

    override fun getUpdatesConsumer() = this

    override fun processNonCommandUpdate(update: Update) {
        receiverService.execute(update)
    }

    @AfterBotRegistration
    fun afterRegistration(botSession: BotSession) {
        logger.info {"Registered bot running state is: ${botSession.isRunning}" }
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}