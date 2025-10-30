package ru.grabovsky.dungeoncrusherbot.strategy.commands

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowEngine
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys

@Component
class SettingsCommand(
    userService: UserService,
    flowEngine: FlowEngine
): AbstractFlowCommand(Command.SETTINGS, FlowKeys.SETTINGS, userService, flowEngine)
