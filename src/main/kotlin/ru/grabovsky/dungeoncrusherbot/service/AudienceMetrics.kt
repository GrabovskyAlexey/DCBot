package ru.grabovsky.dungeoncrusherbot.service

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.repository.UserRepository

/** Аудитория бота как метрики: всего пользователей и тех, кто не заблокировал бота. Читаются из БД при каждом сборе метрик. */
@Component
class AudienceMetrics(
    private val users: UserRepository,
) : MeterBinder {

    override fun bindTo(registry: MeterRegistry) {
        Gauge.builder("bot.users") { users.count().toDouble() }
            .tag("state", "total").description("Пользователи бота").register(registry)
        Gauge.builder("bot.users") { users.countNotBlocked().toDouble() }
            .tag("state", "active").description("Пользователи бота").register(registry)
    }
}
