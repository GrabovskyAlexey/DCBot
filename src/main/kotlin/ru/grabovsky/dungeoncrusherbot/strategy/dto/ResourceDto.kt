package ru.grabovsky.dungeoncrusherbot.strategy.dto

data class ResourceDto(val servers: List<ServerResourceDto> = emptyList()) {
    fun hasServers() = servers.isNotEmpty()
}