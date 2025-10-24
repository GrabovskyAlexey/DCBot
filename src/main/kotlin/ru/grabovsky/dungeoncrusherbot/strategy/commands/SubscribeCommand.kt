package ru.grabovsky.dungeoncrusherbot.strategy.commands

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowEngine
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Component
class SubscribeCommand(
    userService: UserService,
    eventPublisher: ApplicationEventPublisher,
    flowEngine: FlowEngine,
) : AbstractFlowCommand(Command.SUBSCRIBE, FlowKeys.SUBSCRIBE, userService, eventPublisher, flowEngine)
