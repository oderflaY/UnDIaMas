package com.eter.undiamas.features.estadisticas.domain

import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.RiskLevel
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Busca patrones de riesgo en el historial: qué día de la semana y a qué hora suele
 * complicarse la cosa.
 *
 * Es deliberadamente conservador. Decirle a alguien "los viernes recaes" a partir de dos
 * datos sueltos sería inventar un patrón y sembrar una profecía que se cumple sola, así
 * que exige un mínimo de repeticiones antes de afirmar nada.
 */
class RiskPatternDetector(
    private val minOccurrences: Int = 3,
) {
    private fun isRisky(level: RiskLevel) = level != RiskLevel.VERDE

    /** Día de la semana con más check-ins en riesgo, o null si no hay evidencia suficiente. */
    fun riskiestDay(entries: List<CheckInEntry>, timeZone: TimeZone): DayOfWeek? {
        val counts = entries
            .filter { isRisky(it.riskLevel) }
            .groupingBy { it.answeredAt.toLocalDateTime(timeZone).date.dayOfWeek }
            .eachCount()

        val (day, count) = counts.maxByOrNull { it.value } ?: return null
        return if (count >= minOccurrences) day else null
    }

    /** Hora del día con más check-ins en riesgo, o null si no hay evidencia suficiente. */
    fun riskiestHour(entries: List<CheckInEntry>, timeZone: TimeZone): Int? {
        val counts = entries
            .filter { isRisky(it.riskLevel) }
            .groupingBy { it.answeredAt.toLocalDateTime(timeZone).hour }
            .eachCount()

        val (hour, count) = counts.maxByOrNull { it.value } ?: return null
        return if (count >= minOccurrences) hour else null
    }

    /** Si hoy toca el día que históricamente cuesta más, conviene avisar. */
    fun shouldWarnToday(entries: List<CheckInEntry>, timeZone: TimeZone, today: DayOfWeek): Boolean =
        riskiestDay(entries, timeZone) == today
}

/** Nombre del día en español, para los mensajes de la app. */
fun DayOfWeek.spanishName(): String = when (this) {
    DayOfWeek.MONDAY -> "lunes"
    DayOfWeek.TUESDAY -> "martes"
    DayOfWeek.WEDNESDAY -> "miércoles"
    DayOfWeek.THURSDAY -> "jueves"
    DayOfWeek.FRIDAY -> "viernes"
    DayOfWeek.SATURDAY -> "sábados"
    DayOfWeek.SUNDAY -> "domingos"
    else -> name.lowercase()
}
