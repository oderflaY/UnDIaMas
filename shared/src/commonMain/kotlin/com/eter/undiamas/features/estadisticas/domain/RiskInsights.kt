package com.eter.undiamas.features.estadisticas.domain

import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.RiskLevel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Lecturas derivadas del historial de check-ins: en qué franja horaria aparece el riesgo
 * y cuántas veces la persona atravesó un impulso sin recaer.
 */
class RiskInsights {

    private fun isAtRisk(level: RiskLevel) = level != RiskLevel.VERDE

    /** Conteo de check-ins en riesgo por cada hora del día (0..23). */
    fun riskByHour(entries: List<CheckInEntry>, timeZone: TimeZone): List<Int> {
        val counts = MutableList(24) { 0 }
        entries.filter { isAtRisk(it.riskLevel) }.forEach { entry ->
            val hour = entry.answeredAt.toLocalDateTime(timeZone).hour
            counts[hour] = counts[hour] + 1
        }
        return counts
    }

    /** Hora con más check-ins en riesgo, o null si nunca hubo uno. */
    fun riskiestHour(entries: List<CheckInEntry>, timeZone: TimeZone): Int? {
        val byHour = riskByHour(entries, timeZone)
        val max = byHour.maxOrNull() ?: 0
        if (max == 0) return null
        return byHour.indexOfFirst { it == max }
    }

    /**
     * Cada check-in registrado en amarillo o rojo es un momento en que la persona
     * reconoció el impulso y aun así se detuvo a registrarlo: eso cuenta como superado.
     */
    fun urgesOvercome(entries: List<CheckInEntry>): Int = entries.count { isAtRisk(it.riskLevel) }
}
