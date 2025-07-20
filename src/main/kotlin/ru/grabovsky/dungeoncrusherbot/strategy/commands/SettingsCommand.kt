package ru.grabovsky.dungeoncrusherbot.strategy.commands

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class SettingsCommand(
    eventPublisher: ApplicationEventPublisher,
): AbstractCommand(Command.SETTINGS, eventPublisher)