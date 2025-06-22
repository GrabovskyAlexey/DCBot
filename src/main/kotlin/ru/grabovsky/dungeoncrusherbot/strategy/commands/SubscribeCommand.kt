package ru.grabovsky.dungeoncrusherbot.strategy.commands

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class SubscribeCommand(
    eventPublisher: ApplicationEventPublisher,
): AbstractCommand(Command.SUBSCRIBE, eventPublisher)