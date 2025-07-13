package ru.grabovsky.dungeoncrusherbot.entity

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import ru.grabovsky.dungeoncrusherbot.util.DefaultInstantDeserializer
import java.time.Instant

@Entity
@Table(name = "users", schema = "dc_bot")
data class User(
    @Id
    @Column(name = "id")
    val userId: Long,
    @Column(name = "first_name")
    var firstName: String?,
    @Column(name = "last_name")
    var lastName: String?,
    @Column(name = "username")
    var userName: String?,
    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL])
    var maze: Maze? = null,
    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL])
    var resources: Resources? = null,
    @Column(name = "is_blocked")
    var isBlocked: Boolean = false,
    @CreationTimestamp
    @Column(name = "created_at")
    @JsonSerialize(using= InstantSerializer::class)
    @JsonDeserialize(using= DefaultInstantDeserializer::class)
    val createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonSerialize(using= InstantSerializer::class)
    @JsonDeserialize(using= DefaultInstantDeserializer::class)
    val updatedAt: Instant = Instant.now(),
    @ManyToMany(fetch = FetchType.EAGER, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    @JoinTable(
        name = "user_subscribe",
        schema = "dc_bot",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "server_id")]
    )
    val servers: MutableSet<Server> = hashSetOf(),
    @OneToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "user_id")
    val notificationSubscribe: MutableList<NotificationSubscribe> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (userId != other.userId) return false
        if (firstName != other.firstName) return false
        if (lastName != other.lastName) return false
        if (userName != other.userName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + (firstName?.hashCode() ?: 0)
        result = 31 * result + (lastName?.hashCode() ?: 0)
        result = 31 * result + (userName?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "User(userName=$userName, lastName=$lastName, firstName=$firstName, userId=$userId, createdAt=$createdAt, updatedAt=$updatedAt)"
    }
}