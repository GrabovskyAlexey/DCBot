package ru.grabovsky.dungeoncrusherbot.strategy.dto

data class ExchangeDto(
    val servers: List<Server>,
    val username: String?
) : DataModel {
    val hasServers: Boolean get() = servers.isNotEmpty()

    data class Server(
        val id: Int,
        val main: Boolean,
        val hasRequests: Boolean,
    )
}
