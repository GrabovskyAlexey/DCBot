package ru.grabovsky.dungeoncrusherbot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Бизнес-метрики DCBot: срезы данных из БД (серверы, подписки, обмены, долги, языки). Обновляются раз в минуту
 * агрегирующими запросами; метки только с малым числом значений, без пользователей и их данных.
 * Заблокировавшие бота пользователи не учитываются.
 */
@Component
class BusinessMetrics(
    private val jdbc: JdbcTemplate,
    registry: MeterRegistry,
) {
    private val subscribers = gauge(registry, "bot.server.subscribers", "Подписчики на сервер (игрок может быть подписан на несколько)")
    private val mainPlayers = gauge(registry, "bot.server.main.players", "Игроки, у которых сервер отмечен основным")
    private val subscriptions = gauge(registry, "bot.notification.subscriptions", "Подписки на уведомления: SIEGE enabled = напоминание за 5 минут, MINE enabled = уведомления включены")
    private val exchanges = gauge(registry, "bot.exchange.requests.active", "Активные заявки на обмен")
    private val debts = gauge(registry, "bot.debts", "Долги (записей)")
    private val debtsAmount = gauge(registry, "bot.debts.amount", "Долги (сумма)")
    private val languages = gauge(registry, "bot.users.language", "Пользователи по языку Telegram")

    @Scheduled(initialDelay = 15_000, fixedDelay = 60_000)
    fun refresh() {
        update(subscribers, SUBSCRIBERS_SQL) { Tags.of("server", it.text("name"), "server_id", it.text("server_id")) }
        update(mainPlayers, MAIN_SERVER_SQL) { Tags.of("server", it.text("name"), "server_id", it.text("server_id")) }
        update(subscriptions, SUBSCRIPTIONS_SQL) { Tags.of("type", it.text("type"), "enabled", it.text("enabled")) }
        update(exchanges, EXCHANGES_SQL) {
            Tags.of("type", it.text("type"), "source", it.text("source"), "target", it.text("target"))
        }
        update(languages, LANGUAGES_SQL) { Tags.of("language", it.text("language")) }
        val debtRows = query(DEBTS_SQL)
        if (debtRows != null) {
            val tags = { row: Map<String, Any?> -> Tags.of("direction", row.text("direction"), "resource", row.text("resource")) }
            debts.register(debtRows.map { MultiGauge.Row.of(tags(it), it.number("cnt")) }, true)
            debtsAmount.register(debtRows.map { MultiGauge.Row.of(tags(it), it.number("total")) }, true)
        }
    }

    private fun update(gauge: MultiGauge, sql: String, tags: (Map<String, Any?>) -> Tags) {
        val rows = query(sql) ?: return
        gauge.register(rows.map { MultiGauge.Row.of(tags(it), it.number("cnt")) }, true)
    }

    private fun query(sql: String): List<Map<String, Any?>>? =
        runCatching { jdbc.queryForList(sql) }
            .onFailure { logger.warn(it) { "Business metrics query failed" } }
            .getOrNull()

    private fun Map<String, Any?>.text(key: String): String = this[key]?.toString() ?: "unknown"

    private fun Map<String, Any?>.number(key: String): Number = (this[key] as? Number) ?: 0

    private fun gauge(registry: MeterRegistry, name: String, description: String): MultiGauge =
        MultiGauge.builder(name).description(description).register(registry)

    companion object {
        private val logger = KotlinLogging.logger {}

        private const val NOT_BLOCKED = "COALESCE(p.is_blocked, false) = false"

        const val SUBSCRIBERS_SQL = """
            SELECT s.id AS server_id, s.name AS name, COUNT(*) AS cnt
            FROM dc_bot.user_subscribe us
            JOIN dc_bot.servers s ON s.id = us.server_id
            LEFT JOIN dc_bot.user_profile p ON p.user_id = us.user_id
            WHERE $NOT_BLOCKED
            GROUP BY s.id, s.name"""

        const val MAIN_SERVER_SQL = """
            SELECT s.id AS server_id, s.name AS name, COUNT(*) AS cnt
            FROM dc_bot.user_profile p
            JOIN dc_bot.servers s ON s.id = p.main_server_id
            WHERE p.is_blocked = false
            GROUP BY s.id, s.name"""

        const val SUBSCRIPTIONS_SQL = """
            SELECT n.type AS type, COALESCE(n.enabled, true) AS enabled, COUNT(*) AS cnt
            FROM dc_bot.notification_subscribe n
            LEFT JOIN dc_bot.user_profile p ON p.user_id = n.user_id
            WHERE $NOT_BLOCKED
            GROUP BY n.type, COALESCE(n.enabled, true)"""

        const val EXCHANGES_SQL = """
            SELECT type AS type, source_resource_type AS source, target_resource_type AS target, COUNT(*) AS cnt
            FROM dc_bot.exchange_requests
            WHERE is_active
            GROUP BY type, source_resource_type, target_resource_type"""

        const val DEBTS_SQL = """
            SELECT direction AS direction, resource_type AS resource, COUNT(*) AS cnt, COALESCE(SUM(amount), 0) AS total
            FROM dc_bot.debts
            GROUP BY direction, resource_type"""

        const val LANGUAGES_SQL = """
            SELECT COALESCE(u.language, 'unknown') AS language, COUNT(*) AS cnt
            FROM dc_bot.users u
            LEFT JOIN dc_bot.user_profile p ON p.user_id = u.id
            WHERE $NOT_BLOCKED
            GROUP BY COALESCE(u.language, 'unknown')"""
    }
}
