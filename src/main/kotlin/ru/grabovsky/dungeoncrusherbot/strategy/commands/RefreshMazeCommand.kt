package ru.grabovsky.dungeoncrusherbot.strategy.commands

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import ru.grabovsky.dungeoncrusherbot.entity.Maze
import ru.grabovsky.dungeoncrusherbot.service.interfaces.MazeService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Component
class RefreshMazeCommand(
    eventPublisher: ApplicationEventPublisher,
    private val userService: UserService,
    private val mazeService: MazeService
): AbstractCommand(Command.REFRESH_MAZE, eventPublisher) {

    override fun prepare(user: User, chat: Chat, arguments: Array<out String>) {
        val user = userService.getUser(user.id) ?: userService.createOrUpdateUser(user)
        val maze = user.maze ?: Maze(user = user)
        mazeService.refreshMaze(maze)
    }
}