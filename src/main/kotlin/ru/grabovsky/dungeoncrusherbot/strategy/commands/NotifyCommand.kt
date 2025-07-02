package ru.grabovsky.dungeoncrusherbot.strategy.commands

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class NotifyCommand(
    eventPublisher: ApplicationEventPublisher,
): AbstractCommand(Command.NOTIFY, eventPublisher)