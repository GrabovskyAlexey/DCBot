package ru.grabovsky.dungeoncrusherbot.strategy.flow.maze

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.entity.Location
import ru.grabovsky.dungeoncrusherbot.entity.Maze
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService
import java.util.*
import org.telegram.telegrambots.meta.api.objects.User as TgUser

@Component
class MazeViewService(
    private val userService: UserService,
    private val i18nService: I18nService,
) {

    fun buildMainView(user: TgUser, locale: Locale, showHistory: Boolean): MazeMainView {
        val entity = userService.getUser(user.id)
        val maze = entity?.maze
        val location = maze?.currentLocation ?: Location(0, 0, Direction.CENTER)
        val sameSteps = maze?.sameSteps ?: false
        val history = if (showHistory) {
            maze?.steps.orEmpty().takeLast(HISTORY_LIMIT).map { it.toString() }
        } else {
            emptyList()
        }

        val overview = MazeOverviewModel(
            location = location,
            sameStepsEnabled = sameSteps,
            steps = history,
            showHistory = showHistory,
        )

        val buttons = buildButtons(sameSteps, maze?.steps?.isNotEmpty() == true, locale)
        return MazeMainView(overview, buttons)
    }

    fun ensureMaze(user: TgUser): Maze? {
        val entity = userService.getUser(user.id) ?: return null
        return entity.maze ?: Maze(user = entity).also { entity.maze = it }
    }

    private fun buildButtons(sameSteps: Boolean, hasHistory: Boolean, locale: Locale): List<MazeButton> {
        val buttons = mutableListOf<MazeButton>()
        if (sameSteps) {
            buttons += simpleButton(locale, "buttons.maze.step.left", "\u2196\uFE0F", 0, 0, "MAIN:STEP_SAME_LEFT")
            buttons += simpleButton(locale, "buttons.maze.step.center", "\u2B06\uFE0F", 0, 1, "MAIN:STEP_SAME_CENTER")
            buttons += simpleButton(locale, "buttons.maze.step.right", "\u2197\uFE0F", 0, 2, "MAIN:STEP_SAME_RIGHT")
            buttons += simpleButton(locale, "buttons.maze.same_steps.disable", "\uD83D\uDCF4 Один шаг", 1, 0, "MAIN:TOGGLE_SAME")
        } else {
            buttons += simpleButton(locale, "buttons.maze.step.left", "\u2196\uFE0F", 0, 0, "MAIN:STEP_LEFT")
            buttons += simpleButton(locale, "buttons.maze.step.center", "\u2B06\uFE0F", 0, 1, "MAIN:STEP_CENTER")
            buttons += simpleButton(locale, "buttons.maze.step.right", "\u2197\uFE0F", 0, 2, "MAIN:STEP_RIGHT")
            buttons += simpleButton(locale, "buttons.maze.same_steps.enable", "\uD83D\uDCF4 Несколько шагов", 1, 0, "MAIN:TOGGLE_SAME")
        }
        if (hasHistory) {
            buttons += simpleButton(locale, "buttons.maze.undo", "\u21A9 Отменить шаг", 2, 0, "MAIN:UNDO")
        }
        buttons += simpleButton(locale, "buttons.maze.history", "\uD83E\uDDB6 Последние 20 шагов", 3, 0, "MAIN:HISTORY")
        buttons += simpleButton(locale, "buttons.maze.reset", "\uD83D\uDDD1 Сбросить прогресс", 4, 0, "MAIN:RESET")
        return buttons
    }

    private fun simpleButton(
        locale: Locale,
        code: String,
        default: String,
        row: Int,
        col: Int,
        payload: String
    ): MazeButton = MazeButton(
        label = i18nService.i18n(code, locale, default),
        payload = payload,
        row = row,
        col = col,
    )

    companion object {
        private const val HISTORY_LIMIT = 20
    }
}

data class MazeMainView(
    val overview: MazeOverviewModel,
    val buttons: List<MazeButton>,
)

data class MazeOverviewModel(
    val location: Location?,
    val sameStepsEnabled: Boolean,
    val steps: List<String>,
    val showHistory: Boolean,
) {
    val isComplete: Boolean
        get() = (location?.level ?: 0) >= MAZE_COMPLETION_LEVEL

    companion object {
        private const val MAZE_COMPLETION_LEVEL = 500
    }
}

data class MazeButton(
    val label: String,
    val payload: String,
    val row: Int,
    val col: Int,
)
