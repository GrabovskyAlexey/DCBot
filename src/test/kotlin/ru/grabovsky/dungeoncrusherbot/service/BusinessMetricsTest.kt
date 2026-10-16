package ru.grabovsky.dungeoncrusherbot.service

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.springframework.jdbc.core.JdbcTemplate

class BusinessMetricsTest : ShouldSpec({

    fun jdbc(vararg answers: Pair<String, List<Map<String, Any?>>>) = mockk<JdbcTemplate>().also { mock ->
        every { mock.queryForList(any<String>()) } answers {
            val sql = firstArg<String>()
            answers.firstOrNull { it.first == sql }?.second ?: emptyList()
        }
    }

    should("expose server subscribers, main servers and subscriptions with their labels") {
        val registry = SimpleMeterRegistry()
        val metrics = BusinessMetrics(
            jdbc(
                BusinessMetrics.SUBSCRIBERS_SQL to listOf(mapOf("server_id" to 1, "name" to "S1", "cnt" to 5L)),
                BusinessMetrics.MAIN_SERVER_SQL to listOf(mapOf("server_id" to 1, "name" to "S1", "cnt" to 3L)),
                BusinessMetrics.SUBSCRIPTIONS_SQL to listOf(mapOf("type" to "SIEGE", "enabled" to true, "cnt" to 2L)),
            ),
            registry,
        )

        metrics.refresh()

        registry.get("bot.server.subscribers").tag("server", "S1").tag("server_id", "1").gauge().value() shouldBe 5.0
        registry.get("bot.server.main.players").tag("server", "S1").gauge().value() shouldBe 3.0
        registry.get("bot.notification.subscriptions").tag("type", "SIEGE").tag("enabled", "true").gauge().value() shouldBe 2.0
    }

    should("expose debts as count and amount and drop series that disappeared") {
        val registry = SimpleMeterRegistry()
        var rows = listOf(mapOf("direction" to "OWE", "resource" to "MAP", "cnt" to 2L, "total" to 30L))
        val jdbc = mockk<JdbcTemplate> {
            every { queryForList(any<String>()) } answers { if (firstArg<String>() == BusinessMetrics.DEBTS_SQL) rows else emptyList() }
        }
        val metrics = BusinessMetrics(jdbc, registry)

        metrics.refresh()
        registry.get("bot.debts").tag("direction", "OWE").gauge().value() shouldBe 2.0
        registry.get("bot.debts.amount").tag("resource", "MAP").gauge().value() shouldBe 30.0

        rows = emptyList()
        metrics.refresh()
        registry.find("bot.debts").tag("direction", "OWE").gauge() shouldBe null
    }

    should("keep working when one query fails") {
        val registry = SimpleMeterRegistry()
        val jdbc = mockk<JdbcTemplate> {
            every { queryForList(any<String>()) } answers {
                if (firstArg<String>() == BusinessMetrics.SUBSCRIBERS_SQL) error("boom")
                else listOf(mapOf("language" to "ru", "cnt" to 4L))
            }
        }

        BusinessMetrics(jdbc, registry).refresh()

        registry.get("bot.users.language").tag("language", "ru").gauge().value() shouldBe 4.0
    }
})
