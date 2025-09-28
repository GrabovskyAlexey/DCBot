package ru.grabovsky.dungeoncrusherbot.strategy.commands

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Component
class NotesCommand(
    eventPublisher: ApplicationEventPublisher,
    userService: UserService
): AbstractCommand(Command.NOTES, eventPublisher, userService)