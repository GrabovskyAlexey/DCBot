package ru.grabovsky.dungeoncrusherbot.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Entity
@Table(name = "resources", schema = "dc_bot")
data class Resources(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @OneToOne
    @JoinColumn(name = "user_id")
    val user: User,
    @Column(name = "last_server_id")
    var lastServerId: Int? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data")
    var data: ResourcesData = ResourcesData(),
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "history")
    val history: MutableMap<Int, MutableList<ResourcesHistory>> = mutableMapOf()
)

data class ResourcesData(
    val servers: MutableMap<Int, ServerResourceData> = mutableMapOf()
)

data class ServerResourceData(
    var exchange: String? = null,
    var draadorCount: Int = 0,
    var voidCount: Int = 0,
    var balance: Int = 0,
) {
    fun hasData() = exchange != null || draadorCount != 0 || voidCount != 0 || balance != 0
}

data class ResourcesHistory(
    val date: LocalDate,
    val resource: ResourceType,
    val type: DirectionType,
    val quantity: Int
) {
    override fun toString(): String {
        val resource = when(resource) {
            ResourceType.DRAADOR -> "🪆"
            ResourceType.VOID -> "🟣"
        }
        return when(type) {
            DirectionType.ADD -> "*${date.format(df)}* - $quantity $resource получено"
            DirectionType.INCOMING -> "*${date.format(df)}* - $quantity $resource принято"
            DirectionType.OUTGOING -> "*${date.format(df)}* - $quantity $resource передано"
            DirectionType.TRADE -> "*${date.format(df)}* - $quantity $resource продано"
            DirectionType.REMOVE -> "*${date.format(df)}* - $quantity $resource потрачено"
            DirectionType.CATCH -> "*${date.format(df)}* - $quantity $resource поймано"
        }
    }
}

enum class ResourceType {
    DRAADOR, VOID
}

enum class DirectionType {
    ADD, REMOVE, CATCH, INCOMING, OUTGOING, TRADE
}

val df: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")