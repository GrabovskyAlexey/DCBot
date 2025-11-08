package ru.grabovsky.dungeoncrusherbot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.entity.ExchangeAudit

@Repository
interface ExchangeAuditRepository : JpaRepository<ExchangeAudit, Long>
