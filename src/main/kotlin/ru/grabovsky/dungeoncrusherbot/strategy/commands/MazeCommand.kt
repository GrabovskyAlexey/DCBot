package ru.grabovsky.dungeoncrusherbot.strategy.commands

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowEngine
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKeys

@Component
class MazeCommand(
    userService: UserService,
    flowEngine: FlowEngine
) : AbstractFlowCommand(Command.MAZE, FlowKeys.MAZE, userService,  flowEngine)
