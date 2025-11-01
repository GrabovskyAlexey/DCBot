package ru.grabovsky.dungeoncrusherbot.strategy.dto

data class ServerResourceDto(
    val id: Int,
    val draadorCount: Int = 0,
    val voidCount: Int = 0,
    val balance: Int = 0,
    val exchange: String? = null,
    val history: List<String>? = null,
    val hasHistory: Boolean = false,
    val notifyDisable: Boolean = false,
    val main: Boolean,
    val cbEnabled: Boolean,
    val quickResourceEnabled: Boolean,
    val cbCount: Int,
    val notes: List<String> = emptyList(),
    val hasMain: Boolean,
)
