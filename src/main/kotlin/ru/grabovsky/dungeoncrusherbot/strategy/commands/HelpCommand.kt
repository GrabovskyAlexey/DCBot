package ru.grabovsky.dungeoncrusherbot.strategy.commands

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class HelpCommand(
    eventPublisher: ApplicationEventPublisher,
): AbstractCommand(Command.HELP, eventPublisher)