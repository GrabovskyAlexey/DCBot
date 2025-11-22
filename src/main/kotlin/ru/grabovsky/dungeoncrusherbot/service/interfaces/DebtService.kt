package ru.grabovsky.dungeoncrusherbot.service.interfaces

import ru.grabovsky.dungeoncrusherbot.entity.Debt
import ru.grabovsky.dungeoncrusherbot.entity.DebtDirection
import ru.grabovsky.dungeoncrusherbot.entity.DebtResourceType

data class CreateDebtRequest(
    val userId: Long,
    val direction: DebtDirection,
    val serverId: Int,
    val resourceType: DebtResourceType,
    val amount: Int,
    val counterpartyName: String,
)

interface DebtService {
    fun getDebts(userId: Long): List<Debt>
    fun create(request: CreateDebtRequest): Debt
    fun remove(userId: Long, debtId: Long): Boolean
    fun updateAmount(userId: Long, debtId: Long, amount: Int): Boolean
}
