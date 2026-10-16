package ru.grabovsky.dungeoncrusherbot.service

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository

class AudienceMetricsTest : ShouldSpec({
    should("expose the audience as gauges read from the repository on every scrape") {
        var total = 10L
        val users = mockk<UserRepository> {
            every { count() } answers { total }
            every { countNotBlocked() } returns 7
        }
        val registry = SimpleMeterRegistry()
        AudienceMetrics(users).bindTo(registry)

        registry.get("bot.users").tag("state", "total").gauge().value() shouldBe 10.0
        total = 12
        registry.get("bot.users").tag("state", "total").gauge().value() shouldBe 12.0
        registry.get("bot.users").tag("state", "active").gauge().value() shouldBe 7.0
    }
})
