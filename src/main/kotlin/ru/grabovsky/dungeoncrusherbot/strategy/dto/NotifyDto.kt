package ru.grabovsky.dungeoncrusherbot.strategy.dto

import ru.grabovsky.dungeoncrusherbot.entity.NotificationSubscribe

data class NotifyDto(
    val notifications: List<NotificationSubscribe>
): DataModel