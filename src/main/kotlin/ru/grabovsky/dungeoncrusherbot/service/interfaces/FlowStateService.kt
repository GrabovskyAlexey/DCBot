package ru.grabovsky.dungeoncrusherbot.service.interfaces

import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowKey
import ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine.FlowStateSnapshot

interface FlowStateService {
    fun load(userId: Long, flowKey: FlowKey): FlowStateSnapshot?
    fun save(snapshot: FlowStateSnapshot)
    fun clear(userId: Long, flowKey: FlowKey)
}