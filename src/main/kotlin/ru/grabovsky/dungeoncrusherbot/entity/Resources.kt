package ru.grabovsky.dungeoncrusherbot.entity

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ServerResourceData(
    var exchange: String? = null,
    var exchangeUsername: String? = null,
    var exchangeUserId: Long? = null,
    var draadorCount: Int = 0,
    var voidCount: Int = 0,
    var balance: Int = 0,
    var notifyDisable: Boolean = false,
    var cbCount: Int = 0
) {
    fun hasData(cbEnabled: Boolean) = exchange != null || exchangeUsername != null || draadorCount != 0 || voidCount != 0 || balance != 0 || (cbEnabled && cbCount != 0) || notifyDisable
}

data class ResourcesHistory(
    val date: LocalDate,
    val resource: ResourceType,
    val type: DirectionType,
    val quantity: Int,
    val fromServer: Int? = null,
    val prevDraadorCount: Int? = null,
    val prevVoidCount: Int? = null,
    val prevCbCount: Int? = null,
    val prevBalance: Int? = null,
) {
    override fun toString(): String {
        val postfix = if(fromServer != null) "c $fromServer сервера" else ""
        val resource = when(resource) {
            ResourceType.DRAADOR -> "🪆"
            ResourceType.VOID -> "🟣"
            ResourceType.CB -> "\uD83D\uDE08"
        }
        return when(type) {
            DirectionType.ADD -> "*${date.format(df)}* - $quantity $resource получено $postfix"
            DirectionType.INCOMING -> "*${date.format(df)}* - $quantity $resource принято $postfix"
            DirectionType.OUTGOING -> "*${date.format(df)}* - $quantity $resource передано $postfix"
            DirectionType.TRADE -> "*${date.format(df)}* - $quantity $resource продано $postfix"
            DirectionType.REMOVE -> "*${date.format(df)}* - $quantity $resource потрачено $postfix"
            DirectionType.CATCH -> "*${date.format(df)}* - $quantity $resource поймано $postfix"
        }
    }
}

enum class ResourceType {
    DRAADOR, VOID, CB
}

enum class DirectionType {
    ADD, REMOVE, CATCH, INCOMING, OUTGOING, TRADE
}

val df: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
