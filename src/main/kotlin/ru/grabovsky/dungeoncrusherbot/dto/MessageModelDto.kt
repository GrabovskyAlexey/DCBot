package ru.grabovsky.dungeoncrusherbot.dto

import java.io.ByteArrayInputStream

data class MessageModelDto(
    val message: String,
    val inlineButtons: List<InlineMarkupDataDto>,
    val file: ByteArrayInputStream? = null,
    val replyButtons: List<ReplyMarkupDto> = emptyList()
)
