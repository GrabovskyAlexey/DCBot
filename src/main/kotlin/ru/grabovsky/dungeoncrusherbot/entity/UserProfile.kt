package ru.grabovsky.dungeoncrusherbot.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "user_profile", schema = "dc_bot")
data class UserProfile(
    @Id
    @Column(name = "user_id")
    var userId: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    var user: User? = null,

    @Column(name = "is_blocked", nullable = false)
    var isBlocked: Boolean = false,

    @Column(name = "is_admin", nullable = false)
    var isAdmin: Boolean = false,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", nullable = false)
    var settings: UserSettings = UserSettings(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notes", nullable = false)
    var notes: MutableList<String> = mutableListOf(),

    @Column(name = "main_server_id")
    var mainServerId: Int? = null,

    @Column(name = "locale")
    var locale: String? = null,
)
