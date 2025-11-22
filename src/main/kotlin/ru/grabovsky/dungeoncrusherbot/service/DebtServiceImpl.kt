package ru.grabovsky.dungeoncrusherbot.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.grabovsky.dungeoncrusherbot.entity.Debt
import ru.grabovsky.dungeoncrusherbot.repository.DebtRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.CreateDebtRequest
import ru.grabovsky.dungeoncrusherbot.service.interfaces.DebtService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService
import ru.grabovsky.dungeoncrusherbot.service.interfaces.UserService

@Service
class DebtServiceImpl(
    private val userService: UserService,
    private val serverService: ServerService,
    private val debtRepository: DebtRepository,
) : DebtService {

    @Transactional(readOnly = true)
    override fun getDebts(userId: Long): List<Debt> =
        debtRepository.findAllByUserUserIdOrderByCreatedAtDesc(userId)

    @Transactional
    override fun create(request: CreateDebtRequest): Debt {
        val user = userService.getUser(request.userId)
            ?: throw EntityNotFoundException("User ${request.userId} not found")
        serverService.getServerById(request.serverId)
        val debt = Debt(
            user = user,
            direction = request.direction,
            serverId = request.serverId,
            resourceType = request.resourceType,
            amount = request.amount,
            counterpartyName = request.counterpartyName,
        )
        return debtRepository.save(debt)
    }

    @Transactional
    override fun remove(userId: Long, debtId: Long): Boolean {
        if (!debtRepository.existsByIdAndUserUserId(debtId, userId)) {
            return false
        }
        debtRepository.deleteByIdAndUserUserId(debtId, userId)
        return true
    }

    @Transactional
    override fun updateAmount(userId: Long, debtId: Long, amount: Int): Boolean {
        val debt = debtRepository.findByIdAndUserUserId(debtId, userId) ?: return false
        debt.amount = amount
        debtRepository.save(debt)
        return true
    }
}
