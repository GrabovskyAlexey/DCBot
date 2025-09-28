package ru.grabovsky.dungeoncrusherbot.strategy.commands

enum class Command(
    val command: String,
    val text: String,
    val order: Int
) {
    START("start", "\uD83D\uDFE2 Начать пользоваться ботом", 1),
    SUBSCRIBE("subscribe", "\uD83C\uDFF0 Подписаться на осады", 2),
    MAZE("maze", "\uD83C\uDF00 Лабиринт", 3),
    RESOURCES("resources", "\uD83D\uDCCA Учет ресурсов", 4),
    NOTES("notes", "\uD83D\uDDD2 Заметки", 5),
    EXCHANGE("exchange", "\uD83D\uDCB1 Поиск обменников", 6),
    SETTINGS("settings", "⚙\uFE0F Настройки", 7),
    HELP("help", "❓ Помощь", 99);
}
