package ru.grabovsky.dungeoncrusherbot.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

class BotMetricsTest : ShouldSpec({
    val registry = SimpleMeterRegistry()
    val metrics = BotMetrics(registry)

    fun count(name: String, vararg tags: String) = registry.find(name).tags(*tags).timer()?.count() ?: 0L

    should("record a successful block with the ok outcome and return its result") {
        metrics.update("callback") { 42 } shouldBe 42

        count(BotMetrics.UPDATE, "type", "callback", "outcome", "ok") shouldBe 1L
    }

    should("record a failing job with the error outcome and rethrow the exception") {
        shouldThrow<IllegalStateException> { metrics.job("siege") { error("boom") } }

        count(BotMetrics.JOB, "job", "siege", "outcome", "error") shouldBe 1L
        count(BotMetrics.JOB, "job", "siege", "outcome", "ok") shouldBe 0L
    }
})
