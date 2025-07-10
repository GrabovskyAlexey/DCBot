package ru.grabovsky.dungeoncrusherbot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.entity.VerificationRequest

@Repository
interface VerificationRequestRepository: JpaRepository<VerificationRequest, Long>