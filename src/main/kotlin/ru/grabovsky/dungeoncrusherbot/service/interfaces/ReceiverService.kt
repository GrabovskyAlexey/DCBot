package ru.grabovsky.dungeoncrusherbot.service.interfaces

import org.telegram.telegrambots.meta.api.objects.Update

interface ReceiverService {
    fun execute(update: Update)
}