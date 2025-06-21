package ru.grabovsky.dungeoncrusherbot.bot.commands

import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand

abstract class AbstractCommand(
    command: Command,
): BotCommand(command.command, command.text)