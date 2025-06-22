package ru.grabovsky.dungeoncrusherbot.strategy.commands

enum class Command(
    val command: String,
    val text: String,
    val order: Int
) {
    START("start", "Начать пользоваться ботом", 1),
    SUBSCRIBE("subscribe", "Подписаться на осады", 2),
    MAZE("maze", "Лабиринт", 3),
    REFRESH_MAZE("refresh_maze", "Сбросить прогресс прохождения лабиринта", 4),
    HELP("help", "Помощь", 99);
}