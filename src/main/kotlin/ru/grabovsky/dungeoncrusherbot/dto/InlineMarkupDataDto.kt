package ru.grabovsky.dungeoncrusherbot.dto

data class InlineMarkupDataDto(
    val rowPos: Int = 0,
    val text: String,
    val data: CallbackObject
)
