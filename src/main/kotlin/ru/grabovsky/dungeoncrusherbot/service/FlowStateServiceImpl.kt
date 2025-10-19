package ru.grabovsky.dungeoncrusherbot.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.grabovsky.dungeoncrusherbot.entity.FlowState
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKey
import ru.grabovsky.dungeoncrusherbot.service.interfaces.FlowStateService
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStateSnapshot
import ru.grabovsky.dungeoncrusherbot.repository.FlowStateRepository

@Service
class FlowStateServiceImpl(
    private val repository: FlowStateRepository,
) : FlowStateService {

    @Transactional(readOnly = true)
    override fun load(userId: Long, flowKey: FlowKey): FlowStateSnapshot? {
        val entity = repository.findByUserIdAndFlowKey(userId, flowKey.value) ?: return null
        return FlowStateSnapshot(
            userId = entity.userId,
            flowKey = FlowKey(entity.flowKey),
            stepKey = entity.stepKey,
            payload = entity.payload,
            messageBindings = entity.messageBindings?.toMap().orEmpty(),
        )
    }

    @Transactional
    override fun save(snapshot: FlowStateSnapshot) {
        val entity = repository.findByUserIdAndFlowKey(snapshot.userId, snapshot.flowKey.value)
            ?: FlowState(
                userId = snapshot.userId,
                flowKey = snapshot.flowKey.value,
                stepKey = snapshot.stepKey,
            )
        entity.stepKey = snapshot.stepKey
        entity.payload = snapshot.payload
        entity.messageBindings = snapshot.messageBindings.takeIf { it.isNotEmpty() }?.toMutableMap()
        repository.save(entity)
    }

    @Transactional
    override fun clear(userId: Long, flowKey: FlowKey) {
        repository.deleteByUserIdAndFlowKey(userId, flowKey.value)
    }
}