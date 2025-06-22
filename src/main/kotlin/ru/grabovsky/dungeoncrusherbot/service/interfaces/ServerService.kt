package ru.grabovsky.dungeoncrusherbot.service.interfaces

import ru.grabovsky.dungeoncrusherbot.entity.Server

interface ServerService {
    fun getServerById(serverId: Int): Server
    fun getAllServers(): List<Server>
}