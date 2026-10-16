package ru.grabovsky.dungeoncrusherbot.service

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component

/**
 * Метрики бота для Prometheus (`/actuator/prometheus`). Таймеры называются `bot.*` и получают тег `outcome`
 * (`ok` или `error`), поэтому долю ошибок и перцентили можно считать по любому из них; приложение различается тегом `application`.
 */
@Component
class BotMetrics(
    private val registry: MeterRegistry,
) {

    /** Выполняет [block] и записывает время в таймер [name] с тегами [tags] (пары «имя, значение»); исключение считается ошибкой и пробрасывается. */
    fun <T> timed(name: String, vararg tags: String, block: () -> T): T {
        val sample = Timer.start(registry)
        var outcome = OK
        try {
            return block()
        } catch (error: Throwable) {
            outcome = ERROR
            throw error
        } finally {
            sample.stop(Timer.builder(name).tags(*tags, OUTCOME, outcome).register(registry))
        }
    }

    /** Обработка апдейта Telegram: [type] - `callback`, `message`, `member` или `other`. */
    fun <T> update(type: String, block: () -> T): T = timed(UPDATE, "type", type, block = block)

    /** Запуск задачи по расписанию [job]. */
    fun <T> job(job: String, block: () -> T): T = timed(JOB, "job", job, block = block)

    /** Работа flow: [event] - `start`, `callback` или `message`. */
    fun <T> flow(flow: String, event: String, block: () -> T): T = timed(FLOW, "flow", flow, "event", event, block = block)

    /** Отправленное уведомление: [type] - тип подписки, [delivered] - дошло ли оно (иначе пользователь заблокировал бота). */
    fun notification(type: String, delivered: Boolean) {
        Counter.builder(NOTIFICATIONS).tags("type", type, "result", if (delivered) "delivered" else "blocked")
            .register(registry).increment()
    }

    /** Новый пользователь бота (первое обращение). */
    fun newUser() {
        Counter.builder(NEW_USERS).register(registry).increment()
    }

    companion object {
        const val UPDATE = "bot.update"
        const val JOB = "bot.job"
        const val FLOW = "bot.flow"
        const val NOTIFICATIONS = "bot.notifications"
        const val NEW_USERS = "bot.new.users"
        private const val OUTCOME = "outcome"
        private const val OK = "ok"
        private const val ERROR = "error"
    }
}
