package ru.grabovsky.dungeoncrusherbot.strategy.processor

import ru.grabovsky.dungeoncrusherbot.util.CommonUtils.currentStateCode


interface Processor {
    fun classStateCode() = this.currentStateCode("Processor")
}
