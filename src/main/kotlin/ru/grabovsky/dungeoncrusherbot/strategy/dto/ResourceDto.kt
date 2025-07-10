package ru.grabovsky.dungeoncrusherbot.strategy.dto

data class ResourceDto(val servers: List<ServerResourceDto> = emptyList()): DataModel {
    fun hasServers() = servers.isNotEmpty()
}