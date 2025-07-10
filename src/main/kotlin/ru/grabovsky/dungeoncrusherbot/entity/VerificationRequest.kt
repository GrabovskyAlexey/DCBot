package ru.grabovsky.dungeoncrusherbot.entity

import jakarta.persistence.*
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

@Entity
@Table(name="verification_request", schema = "dc_bot")
data class VerificationRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "message")
    var message: String,
    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    var stateCode: StateCode,
    var result: Boolean = false
)