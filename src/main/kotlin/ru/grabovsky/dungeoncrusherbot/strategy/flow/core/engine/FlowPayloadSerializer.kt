package ru.grabovsky.dungeoncrusherbot.strategy.flow.core.engine

interface FlowPayloadSerializer {
    fun <T : Any> deserialize(payload: String?, type: Class<T>): T
    fun <T : Any> serialize(payload: T?): String?
}
