package ru.grabovsky.dungeoncrusherbot.strategy.flow.admin

import ru.grabovsky.dungeoncrusherbot.strategy.dto.AdminMessageDto
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.support.PromptState

data class AdminMessageFlowState(
    val messages: MutableList<AdminPendingMessage> = mutableListOf(),
    var pendingReply: AdminPendingReply? = null,
    override val promptBindings: MutableList<String> = mutableListOf(),
) : PromptState

data class AdminPendingMessage(
    val id: Long,
    val dto: AdminMessageDto,
    val bindingKey: String,
    val sourceMessageId: Int? = null,
)

data class AdminPendingReply(
    val messageId: Long,
    val bindingKey: String,
    val dto: AdminMessageDto,
    val sourceMessageId: Int? = null,
)
