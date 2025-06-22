package ru.grabovsky.dungeoncrusherbot.strategy.commands

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class MazeCommand(
    eventPublisher: ApplicationEventPublisher,
): AbstractCommand(Command.MAZE, eventPublisher)