package ru.grabovsky.dungeoncrusherbot.strategy.commands

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Component
class HelpCommand(
    eventPublisher: ApplicationEventPublisher,
    userService: UserService
): AbstractCommand(Command.HELP, eventPublisher, userService)