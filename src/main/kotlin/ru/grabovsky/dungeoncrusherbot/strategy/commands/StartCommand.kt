package ru.grabovsky.dungeoncrusherbot.strategy.commands

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowEngine
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys

@Component
class StartCommand(
    userService: UserService,
    eventPublisher: ApplicationEventPublisher,
    flowEngine: FlowEngine
) : AbstractFlowCommand(Command.START, FlowKeys.START, userService, eventPublisher, flowEngine)