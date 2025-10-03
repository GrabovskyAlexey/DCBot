package ru.grabovsky.dungeoncrusherbot.strategy.dto

data class ExchangeDto(
    val username: String?,
    val servers: List<Server>
) : DataModel {
    val hasServers: Boolean get() = servers.isNotEmpty()

    data class Server(
        val id: Int,
        val name: String?,
        val hasExchange: Boolean,
        val exchange: String?,
        val main: Boolean
    )
}
