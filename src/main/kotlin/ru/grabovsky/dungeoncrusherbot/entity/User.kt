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
    val servers: MutableSet<Server> = hashSetOf()
)