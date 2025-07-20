package ru.grabovsky.dungeoncrusherbot.strategy.commands

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class NotesCommand(
    eventPublisher: ApplicationEventPublisher,
): AbstractCommand(Command.NOTES, eventPublisher)