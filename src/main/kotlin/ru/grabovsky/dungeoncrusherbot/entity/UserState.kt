package ru.grabovsky.dungeoncrusherbot.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

@Entity
@Table(name="user_state", schema = "dc_bot")
data class UserState(
    @Id
    val userId: Long,
    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    var state: StateCode,
    @Column(name = "callback_data")
    var callbackData: String? = null,
    @Column(name = "update_message_id")
    var updateMessageId: Int? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "delete_message_ids")
    val deletedMessages: MutableList<Int> = mutableListOf()
)