package ru.grabovsky.dungeoncrusherbot.entity

import jakarta.persistence.*

@Entity
@Table(name = "servers", schema = "dc_bot")
data class Server(
    @Id
    @Column(name = "id")
    val id: Int,
    @Column(name = "name")
    val name: String,
//    @ManyToMany(mappedBy = "servers")
//    val user: MutableSet<User> = mutableSetOf(),
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "server_sieges",
        schema = "dc_bot",
        joinColumns = [JoinColumn(name = "server_id")],
        inverseJoinColumns = [JoinColumn(name = "siege_id")]
    )
    val sieges: MutableSet<Siege> = hashSetOf()


)