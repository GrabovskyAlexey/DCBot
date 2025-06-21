package ru.grabovsky.dungeoncrusherbot.bot.commands

enum class Command(
    val command: String,
    val text: String
) {
    START("start", "Начать пользоваться ботом"),
    SUBSCRIBE("subscribe", "Добавить место");
}