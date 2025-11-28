package ru.grabovsky.dungeoncrusherbot.service.interfaces

import org.telegram.telegrambots.meta.api.objects.User

interface ResourcesService {
    fun applyOperation(user: User, serverId: Int, operation: ResourceOperation)
    fun undoLastOperation(user: User, serverId: Int): Boolean
}
