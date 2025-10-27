package ru.grabovsky.dungeoncrusherbot.strategy.flow.maze

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.entity.Direction
import ru.grabovsky.dungeoncrusherbot.service.interfaces.I18nService
import java.util.Locale

@Component
class MazePromptBuilder(
    private val i18nService: I18nService,
) {

    fun build(direction: Direction, locale: Locale, invalid: Boolean = false): MazePromptModel {
        val directionLabel = i18nService.i18n(
            DIRECTION_LABEL_KEYS.getValue(direction),
            locale,
            DIRECTION_LABEL_DEFAULTS.getValue(direction)
        )
        val base = i18nService.i18n(
            "flow.maze.prompt.same_steps.base",
            locale,
            "Укажите количество шагов {0}",
            directionLabel
        )
        val error = if (invalid) {
            "\n${i18nService.i18n("flow.maze.prompt.same_steps.invalid", locale, "Введите число от 1 до 10")}"
        } else {
            ""
        }
        return MazePromptModel(text = base + error)
    }

    companion object {
        private val DIRECTION_LABEL_KEYS = mapOf(
            Direction.LEFT to "flow.maze.prompt.same_steps.direction.left",
            Direction.CENTER to "flow.maze.prompt.same_steps.direction.center",
            Direction.RIGHT to "flow.maze.prompt.same_steps.direction.right",
        )

        private val DIRECTION_LABEL_DEFAULTS = mapOf(
            Direction.LEFT to "влево",
            Direction.CENTER to "прямо",
            Direction.RIGHT to "вправо",
        )
    }
}

data class MazePromptModel(
    val text: String,
)
