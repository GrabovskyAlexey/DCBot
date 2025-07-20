package ru.grabovsky.dungeoncrusherbot.strategy.commands

enum class Command(
    val command: String,
    val text: String,
    val order: Int
) {
    START("start", "Начать пользоваться ботом", 1),
    SUBSCRIBE("subscribe", "Подписаться на осады", 2),
    MAZE("maze", "Лабиринт", 3),
    RESOURCES("resources", "Учет ресурсов", 4),
    NOTES("notes", "Заметки", 5),
    SETTINGS("settings", "Настройки", 6),
    HELP("help", "Помощь", 99);
}